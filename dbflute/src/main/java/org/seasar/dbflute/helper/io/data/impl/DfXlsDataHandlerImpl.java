/*
 * Copyright 2004-2008 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.dbflute.helper.io.data.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.seasar.dbflute.exception.TableDataRegistrationFailureException;
import org.seasar.dbflute.exception.TableNotFoundException;
import org.seasar.dbflute.helper.collection.DfFlexibleMap;
import org.seasar.dbflute.helper.dataset.DataColumn;
import org.seasar.dbflute.helper.dataset.DataRow;
import org.seasar.dbflute.helper.dataset.DataSet;
import org.seasar.dbflute.helper.dataset.DataTable;
import org.seasar.dbflute.helper.dataset.states.CreatedState;
import org.seasar.dbflute.helper.dataset.states.SqlContext;
import org.seasar.dbflute.helper.dataset.types.ColumnType;
import org.seasar.dbflute.helper.dataset.types.ColumnTypes;
import org.seasar.dbflute.helper.io.data.DfXlsDataHandler;
import org.seasar.dbflute.helper.io.text.DfMapStringFileReader;
import org.seasar.dbflute.helper.io.xls.DfXlsReader;
import org.seasar.dbflute.helper.jdbc.metadata.info.DfColumnMetaInfo;

/**
 * @author jflute
 */
public class DfXlsDataHandlerImpl extends DfAbsractDataWriter implements DfXlsDataHandler {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** Log instance. */
    private static final Log _log = LogFactory.getLog(DfSeparatedDataHandlerImpl.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** Does it output the insert SQLs as logging? */
    protected boolean _loggingInsertSql;

    /** The name of schema. (Nullable) */
    protected String _schemaName;

    /** The pattern of skip sheet. (Nullable) */
    protected Pattern _skipSheetPattern;

    /** The cache map of meta info. The key is table name. */
    protected Map<String, DfFlexibleMap<String, DfColumnMetaInfo>> _metaInfoCacheMap = new HashMap<String, DfFlexibleMap<String, DfColumnMetaInfo>>();

    // ===================================================================================
    //                                                                                Read
    //                                                                                ====
    public List<DataSet> readSeveralData(String dataDirectoryName) {
        final List<File> xlsList = getXlsList(dataDirectoryName);
        final List<DataSet> ls = new ArrayList<DataSet>();
        for (File file : xlsList) {
            final DfXlsReader xlsReader = createXlsReader(dataDirectoryName, file);
            ls.add(xlsReader.read());
        }
        return ls;
    }

    // ===================================================================================
    //                                                                               Write
    //                                                                               =====
    public void writeSeveralData(String dataDirectoryName, final DataSource dataSource) {
        final List<File> xlsList = getXlsList(dataDirectoryName);

        for (File file : xlsList) {
            _log.info("");
            _log.info("/= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = ");
            _log.info("writeData(" + file + ")");
            _log.info("= = = = = = =/");
            final DfXlsReader xlsReader = createXlsReader(dataDirectoryName, file);
            final DataSet dataSet = xlsReader.read();

            filterValidColumn(dataSet, dataSource);
            setupDefaultValue(dataDirectoryName, dataSet, dataSource);

            for (int i = 0; i < dataSet.getTableSize(); i++) {
                final DataTable dataTable = dataSet.getTable(i);
                final String tableName = dataTable.getTableName();
                if (dataTable.getRowSize() == 0) {
                    _log.info("*Not found row at the table: " + tableName);
                    continue;
                }

                // Set up columnMetaInfo.
                final DfFlexibleMap<String, DfColumnMetaInfo> columnMetaInfoMap = getColumnMetaInfo(dataSource,
                        tableName);

                // Extension Point as Before.
                beforeHandlingTable(dataSource, dataTable);

                // Set up columnNameList.
                final List<String> columnNameList = new ArrayList<String>();
                for (int j = 0; j < dataTable.getColumnSize(); j++) {
                    final DataColumn dataColumn = dataTable.getColumn(j);
                    final String columnName = dataColumn.getColumnName();
                    columnNameList.add(columnName);
                }

                PreparedStatement ps = null;
                try {
                    for (int j = 0; j < dataTable.getRowSize(); j++) {
                        final DataRow dataRow = dataTable.getRow(j);
                        if (ps == null) {
                            final MyCreatedState myCreatedState = new MyCreatedState();
                            final String preparedSql = myCreatedState.buildPreparedSql(dataRow);
                            ps = dataSource.getConnection().prepareStatement(preparedSql);
                        }

                        // ColumnValue and ColumnObject
                        final ColumnContainer columnContainer = createColumnContainer(dataTable, dataRow);
                        final Map<String, Object> columnValueMap = columnContainer.getColumnValueMap();
                        if (columnValueMap.isEmpty()) {
                            String msg = "The table was Not Found in the file:";
                            msg = msg + " tableName=" + tableName + " file=" + file;
                            throw new TableNotFoundException(msg);
                        }
                        if (_loggingInsertSql) {
                            final List<Object> valueList = new ArrayList<Object>(columnValueMap.values());
                            _log.info(getSql4Log(tableName, columnNameList, valueList));
                        }
                        int bindCount = 1;
                        final Set<String> columnNameSet = columnValueMap.keySet();
                        for (String columnName : columnNameSet) {
                            final Object obj = columnValueMap.get(columnName);

                            // If the value is not null and the value has the own type except string,
                            // It registers the value to statement by the type.
                            if (processNotNullNotString(columnName, obj, ps, bindCount)) {
                                bindCount++;
                                continue;
                            }

                            // - - - - - - - - - - - - - - 
                            // Against Null Headache
                            // - - - - - - - - - - - - - -
                            if (processNull(columnName, obj, ps, bindCount, columnMetaInfoMap)) {
                                bindCount++;
                                continue;
                            }

                            // * * * * * * * * * * * * * * * *
                            //       Here String Only
                            // * * * * * * * * * * * * * * * *
                            String value = (String) obj;

                            // - - - - - - - - - - - - - - - - - - -
                            // Remove double quotation if it exists.
                            // - - - - - - - - - - - - - - - - - - -
                            if (value != null && value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
                                value = removeDoubleQuotation(value);
                            }

                            // - - - - - - - - - - - - - - 
                            // Against Time Headache
                            // - - - - - - - - - - - - - -
                            if (processTime(columnName, value, ps, bindCount, columnMetaInfoMap)) {
                                bindCount++;
                                continue;
                            }

                            // - - - - - - - - - - - - - - 
                            // Against Timestamp Headache
                            // - - - - - - - - - - - - - -
                            if (processTimestamp(columnName, value, ps, bindCount, columnMetaInfoMap)) {
                                bindCount++;
                                continue;
                            }

                            // - - - - - - - - - - - - - - 
                            // Against Boolean Headache
                            // - - - - - - - - - - - - - -
                            if (processBoolean(columnName, value, ps, bindCount, columnMetaInfoMap)) {
                                bindCount++;
                                continue;
                            }

                            // - - - - - - - - - - - - - - 
                            // Against Number Headache
                            // - - - - - - - - - - - - - -
                            if (processNumber(columnName, value, ps, bindCount, columnMetaInfoMap)) {
                                bindCount++;
                                continue;
                            }

                            ps.setString(bindCount, value);
                            bindCount++;
                        }
                        ps.addBatch();
                    }
                    if (ps == null) {
                        String msg = "The statement should not be null:";
                        msg = msg + " currentTable=" + dataTable.getTableName();
                        msg = msg + " rowSize=" + dataTable.getRowSize();
                        throw new IllegalStateException(msg);
                    }
                    ps.executeBatch();
                } catch (SQLException e) {
                    final SQLException nextEx = e.getNextException();
                    if (nextEx != null) {
                        _log.warn("");
                        _log.warn("/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * ");
                        _log.warn("SQLException was thrown! getNextException()=" + nextEx.getClass(), nextEx);
                        _log.warn("* * * * * * * * * */");
                        _log.warn("");
                    }
                    String msg = "Failed to register the table data: " + tableName;
                    throw new TableDataRegistrationFailureException(msg, e);
                } finally {
                    if (ps != null) {
                        try {
                            ps.close();
                        } catch (SQLException ignored) {
                            _log.info("statement.close() threw the exception!", ignored);
                        }
                    }
                    // Extension Point as Finally.
                    finallyHandlingTable(dataSource, dataTable);
                }
            }
        }
    }

    protected void beforeHandlingTable(DataSource dataSource, DataTable dataTable) {
    }

    protected void finallyHandlingTable(DataSource dataSource, DataTable dataTable) {
    }

    protected String removeDoubleQuotation(String value) {
        value = value.substring(1);
        value = value.substring(0, value.length() - 1);
        return value;
    }

    // ===================================================================================
    //                                                                    Column Meta Info
    //                                                                    ================
    protected DfFlexibleMap<String, DfColumnMetaInfo> getColumnMetaInfo(DataSource dataSource, String tableName) {
        if (_metaInfoCacheMap.containsKey(tableName)) {
            return _metaInfoCacheMap.get(tableName);
        }
        final DfFlexibleMap<String, DfColumnMetaInfo> columnMetaInfoMap = new DfFlexibleMap<String, DfColumnMetaInfo>();
        try {
            final DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
            final List<DfColumnMetaInfo> columnMetaDataList = _columnHandler.getColumns(metaData, _schemaName,
                    tableName);
            for (DfColumnMetaInfo columnMetaInfo : columnMetaDataList) {
                columnMetaInfoMap.put(columnMetaInfo.getColumnName(), columnMetaInfo);
            }
            _metaInfoCacheMap.put(tableName, columnMetaInfoMap);
            return columnMetaInfoMap;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
    
    // ===================================================================================
    //                                                                        Xls Handling
    //                                                                        ============
    protected DfXlsReader createXlsReader(String dataDirectoryName, File file) {
        final DfFlexibleMap<String, String> tableNameMap = getTableNameMap(dataDirectoryName);
        final DfFlexibleMap<String, List<String>> notTrimTableColumnMap = getNotTrimTableColumnMap(dataDirectoryName);
        final DfFlexibleMap<String, List<String>> emptyStringTableColumnMap = getEmptyStringTableColumnMap(dataDirectoryName);
        final DfXlsReader xlsReader = new DfXlsReader(file, tableNameMap, notTrimTableColumnMap,
                emptyStringTableColumnMap, _skipSheetPattern);
        if (tableNameMap != null && !tableNameMap.isEmpty()) {
            _log.info("/- - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
            _log.info("tableNameMap = " + tableNameMap);
            _log.info("- - - - - - - - - -/");
        }
        return xlsReader;
    }

    public List<File> getXlsList(String dataDirectoryName) {
        final Comparator<File> fileNameAscComparator = new Comparator<File>() {
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
        final SortedSet<File> sortedFileSet = new TreeSet<File>(fileNameAscComparator);

        final File dir = new File(dataDirectoryName);
        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xls");
            }
        };
        final File[] listFiles = dir.listFiles(filter);
        if (listFiles == null) {
            return new ArrayList<File>();
        }
        for (File file : listFiles) {
            sortedFileSet.add(file);
        }
        return new ArrayList<File>(sortedFileSet);
    }

    protected void filterValidColumn(final DataSet dataSet, final DataSource dataSource) {
        for (int i = 0; i < dataSet.getTableSize(); i++) {
            final DataTable table = dataSet.getTable(i);
            final String tableName = table.getTableName();

            final DfFlexibleMap<String, DfColumnMetaInfo> metaInfoMap = getColumnMetaInfo(dataSource, tableName);
            for (int j = 0; j < table.getColumnSize(); j++) {
                final DataColumn dataColumn = table.getColumn(j);
                if (!metaInfoMap.containsKey(dataColumn.getColumnName())) {
                    dataColumn.setWritable(false);
                }
            }
        }
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    protected void setupDefaultValue(String dataDirectoryName, final DataSet dataSet, final DataSource dataSource) {
        final Map<String, String> defaultValueMap = getDefaultValueMap(dataDirectoryName);
        for (int i = 0; i < dataSet.getTableSize(); i++) {
            final DataTable table = dataSet.getTable(i);
            final Set<String> defaultValueMapKeySet = defaultValueMap.keySet();
            final String tableName = table.getTableName();

            final DfFlexibleMap<String, DfColumnMetaInfo> metaInfoMap = getColumnMetaInfo(dataSource, tableName);
            for (String defaultTargetColumnName : defaultValueMapKeySet) {
                final String defaultValue = defaultValueMap.get(defaultTargetColumnName);

                if (metaInfoMap.containsKey(defaultTargetColumnName) && !table.hasColumn(defaultTargetColumnName)) {
                    final ColumnType columnType;
                    final Object value;
                    if (defaultValue.equalsIgnoreCase("sysdate")) {
                        columnType = ColumnTypes.TIMESTAMP;
                        value = new Timestamp(System.currentTimeMillis());
                    } else {
                        columnType = ColumnTypes.STRING;
                        value = defaultValue;
                    }
                    table.addColumn(defaultTargetColumnName, columnType);

                    int rowSize = table.getRowSize();
                    for (int j = 0; j < table.getRowSize(); j++) {
                        final DataRow row = table.getRow(j);
                        row.addValue(defaultTargetColumnName, value);
                        ++rowSize;
                    }
                }
            }
        }
    }

    private Map<String, String> getDefaultValueMap(String dataDirectoryName) {
        final DfMapStringFileReader reader = new DfMapStringFileReader();
        String path = dataDirectoryName + "/defaultValueMap.dataprop";
        Map<String, String> resultMap = reader.readMapAsStringValue(path, "UTF-8");
        if (resultMap != null && !resultMap.isEmpty()) {
            return resultMap;
        }
        path = dataDirectoryName + "/default-value.txt";
        resultMap = reader.readMapAsStringValue(path, "UTF-8");
        return resultMap;
    }

    private DfFlexibleMap<String, String> getTableNameMap(String dataDirectoryName) {
        final DfMapStringFileReader reader = new DfMapStringFileReader();
        String path = dataDirectoryName + "/tableNameMap.dataprop";
        Map<String, String> resultMap = reader.readMapAsStringValue(path, "UTF-8");
        if (resultMap != null && !resultMap.isEmpty()) {
            return new DfFlexibleMap<String, String>(resultMap);
        }
        path = dataDirectoryName + "/table-name.txt";
        resultMap = reader.readMapAsStringValue(path, "UTF-8");
        return new DfFlexibleMap<String, String>(resultMap);
    }

    private DfFlexibleMap<String, List<String>> getNotTrimTableColumnMap(String dataDirectoryName) {
        final DfMapStringFileReader reader = new DfMapStringFileReader();
        String path = dataDirectoryName + "/notTrimColumnMap.dataprop";
        Map<String, List<String>> resultMap = reader.readMapAsListStringValue(path, "UTF-8");
        if (resultMap != null && !resultMap.isEmpty()) {
            return new DfFlexibleMap<String, List<String>>(resultMap);
        }
        path = dataDirectoryName + "/not-trim-column.txt";
        resultMap = reader.readMapAsListStringValue(path, "UTF-8");
        return new DfFlexibleMap<String, List<String>>(resultMap);
    }

    private DfFlexibleMap<String, List<String>> getEmptyStringTableColumnMap(String dataDirectoryName) {
        final DfMapStringFileReader reader = new DfMapStringFileReader();
        String path = dataDirectoryName + "/emptyStringColumnMap.dataprop";
        Map<String, List<String>> resultMap = reader.readMapAsListStringValue(path, "UTF-8");
        if (resultMap != null && !resultMap.isEmpty()) {
            return new DfFlexibleMap<String, List<String>>(resultMap);
        }
        path = dataDirectoryName + "/empty-string-column.txt";
        resultMap = reader.readMapAsListStringValue(path, "UTF-8");
        return new DfFlexibleMap<String, List<String>>(resultMap);
    }

    protected String getSql4Log(String tableName, List<String> columnNameList,
            final List<? extends Object> bindParameters) {
        String bindParameterString = bindParameters.toString();
        bindParameterString = bindParameterString.substring(1, bindParameterString.length() - 1);
        return tableName + ":{" + bindParameterString + "}";
    }

    protected ColumnContainer createColumnContainer(final DataTable dataTable, final DataRow dataRow) {
        final ColumnContainer container = new ColumnContainer();
        for (int i = 0; i < dataTable.getColumnSize(); i++) {
            final DataColumn dataColumn = dataTable.getColumn(i);
            if (!dataColumn.isWritable()) {
                continue;
            }
            final Object value = dataRow.getValue(i);
            final String columnName = dataColumn.getColumnName();
            container.addColumnValue(columnName, value);
            container.addColumnObject(columnName, dataColumn);
        }
        return container;
    }

    // ===================================================================================
    //                                                                        Helper Class
    //                                                                        ============
    protected static class ColumnContainer {
        protected Map<String, Object> columnValueMap = new LinkedHashMap<String, Object>();
        protected Map<String, DataColumn> columnObjectMap = new LinkedHashMap<String, DataColumn>();

        public Map<String, Object> getColumnValueMap() {
            return columnValueMap;
        }

        public void addColumnValue(String columnName, Object columnValue) {
            this.columnValueMap.put(columnName, columnValue);
        }

        public Map<String, DataColumn> getColumnObjectMap() {
            return columnObjectMap;
        }

        public void addColumnObject(String columnName, DataColumn columnObject) {
            this.columnObjectMap.put(columnName, columnObject);
        }
    }

    protected static class MyCreatedState {
        public String buildPreparedSql(final DataRow row) {
            final CreatedState createdState = new CreatedState() {
                public String toString() {
                    final SqlContext sqlContext = getSqlContext(row);
                    return sqlContext.getSql();
                }
            };
            return createdState.toString();
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean isLoggingInsertSql() {
        return _loggingInsertSql;
    }

    public void setLoggingInsertSql(boolean loggingInsertSql) {
        this._loggingInsertSql = loggingInsertSql;
    }

    public String getSchemaName() {
        return _schemaName;
    }

    public void setSchemaName(String schemaName) {
        _schemaName = schemaName;
    }

    public void setSkipSheet(String skipSheet) {
        if (skipSheet == null || skipSheet.trim().length() == 0) {
            return;
        }
        try {
            _skipSheetPattern = Pattern.compile(skipSheet);
        } catch (PatternSyntaxException e) {
            String msg = "The pattern syntax for skip-sheet was wrong: " + skipSheet;
            throw new IllegalStateException(msg, e);
        }
    }
}
