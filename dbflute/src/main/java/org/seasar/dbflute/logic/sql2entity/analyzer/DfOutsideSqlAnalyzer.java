/*
 * Copyright 2004-2011 the Seasar Foundation and the Others.
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
package org.seasar.dbflute.logic.sql2entity.analyzer;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.engine.database.model.AppData;
import org.seasar.dbflute.DBDef;
import org.seasar.dbflute.DfBuildProperties;
import org.seasar.dbflute.exception.DfCustomizeEntityDuplicateException;
import org.seasar.dbflute.exception.DfParameterBeanDuplicateException;
import org.seasar.dbflute.exception.factory.ExceptionMessageBuilder;
import org.seasar.dbflute.helper.StringKeyMap;
import org.seasar.dbflute.helper.jdbc.DfRunnerInformation;
import org.seasar.dbflute.helper.jdbc.sqlfile.DfSqlFileRunnerBase;
import org.seasar.dbflute.logic.jdbc.metadata.info.DfColumnMetaInfo;
import org.seasar.dbflute.logic.sql2entity.bqp.DfBehaviorQueryPathSetupper;
import org.seasar.dbflute.logic.sql2entity.cmentity.DfCustomizeEntityInfo;
import org.seasar.dbflute.logic.sql2entity.cmentity.DfCustomizeEntityMetaExtractor;
import org.seasar.dbflute.logic.sql2entity.cmentity.DfCustomizeEntityMetaExtractor.DfForcedJavaNativeProvider;
import org.seasar.dbflute.logic.sql2entity.pmbean.DfPmbMetaData;
import org.seasar.dbflute.logic.sql2entity.pmbean.DfPropertyTypePackageResolver;
import org.seasar.dbflute.properties.DfBasicProperties;
import org.seasar.dbflute.properties.DfDatabaseProperties;
import org.seasar.dbflute.util.Srl;

/**
 * @author jflute
 */
public class DfOutsideSqlAnalyzer extends DfSqlFileRunnerBase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** Log instance. */
    private static final Log _log = LogFactory.getLog(DfOutsideSqlAnalyzer.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final DfSql2EntityMeta _sql2entityMeta;
    protected final DBDef _currentDBDef;
    protected final AppData _schemaData;

    protected final DfSql2EntityMarkAnalyzer _outsideSqlMarkAnalyzer = new DfSql2EntityMarkAnalyzer();
    protected final DfSqlFileNameResolver _sqlFileNameResolver = new DfSqlFileNameResolver();
    protected final DfPropertyTypePackageResolver _propertyTypePackageResolver = new DfPropertyTypePackageResolver();
    protected final DfBehaviorQueryPathSetupper _bqpSetupper = new DfBehaviorQueryPathSetupper();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public DfOutsideSqlAnalyzer(DfRunnerInformation runInfo, DataSource dataSource, DfSql2EntityMeta sqlFileMeta,
            AppData schemaData) {
        super(runInfo, dataSource);
        _sql2entityMeta = sqlFileMeta;
        _currentDBDef = currentDBDef();
        _schemaData = schemaData;
    }

    // ===================================================================================
    //                                                                              Filter
    //                                                                              ======
    /**
     * Filter the string of SQL. Resolve JDBC dependency.
     * @param sql The string of SQL. (NotNull)
     * @return The filtered string of SQL. (NotNull)
     */
    @Override
    protected String filterSql(String sql) {
        // the comments are special mark for Sql2Entity
        // so this timing to do is bad because the special mark is removed.
        //if (!currentDBDef.dbway().isBlockCommentSupported()) {
        //    sql = removeBlockComment(sql);
        //}
        //if (!currentDBDef.dbway().isLineCommentSupported()) {
        //    sql = removeLineComment(sql);
        //}
        return super.filterSql(sql);
    }

    // ===================================================================================
    //                                                                           Execution
    //                                                                           =========
    @Override
    protected void execSQL(String sql) {
        ResultSet rs = null;
        try {
            DfCustomizeEntityInfo customizeEntityInfo = null;
            boolean alreadyIncrementGoodSqlCount = false;
            if (isTargetEntityMakingSql(sql)) {
                final String executedActuallySql;
                {
                    // the timing to remove comments is here
                    String filtered = sql;
                    if (!_currentDBDef.dbway().isBlockCommentSupported()) {
                        filtered = removeBlockComment(filtered);
                    }
                    if (!_currentDBDef.dbway().isLineCommentSupported()) {
                        filtered = removeLineComment(filtered);
                    }
                    executedActuallySql = filtered;
                }
                checkStatement(executedActuallySql);
                rs = _currentStatement.executeQuery(executedActuallySql);

                _goodSqlCount++;
                alreadyIncrementGoodSqlCount = true;

                final Map<String, String> columnForcedJavaNativeMap = createColumnForcedJavaNativeMap(sql);
                final DfCustomizeEntityMetaExtractor customizeEntityMetaExtractor = new DfCustomizeEntityMetaExtractor();
                final Map<String, DfColumnMetaInfo> columnMetaInfoMap = customizeEntityMetaExtractor
                        .extractColumnMetaInfoMap(rs, sql, new DfForcedJavaNativeProvider() {
                            public String provide(String columnName) {
                                return columnForcedJavaNativeMap.get(columnName);
                            }
                        });

                // for Customize Entity
                String entityName = getCustomizeEntityName(sql);
                if (entityName != null) {
                    entityName = resolveEntityNameIfNeeds(entityName, _sqlFile);
                    assertDuplicateEntity(entityName, _sqlFile);
                    // saves for setting to pmbMetaData
                    customizeEntityInfo = new DfCustomizeEntityInfo(entityName, columnMetaInfoMap);
                    customizeEntityInfo.setSqlFile(_sqlFile);
                    if (isDomain(sql)) {
                        customizeEntityInfo.setDomainHandling(true);
                    } else if (isCursor(sql)) {
                        customizeEntityInfo.setCursorHandling(true);
                    } else if (isScalar(sql)) {
                        customizeEntityInfo.setScalarHandling(true);
                    }
                    customizeEntityInfo.setPrimaryKeyList(getPrimaryKeyColumnNameList(sql));
                    _sql2entityMeta.addEntityInfo(entityName, customizeEntityInfo);
                }
            }
            if (isTargetParameterBeanMakingSql(sql)) {
                if (!alreadyIncrementGoodSqlCount) {
                    _goodSqlCount++;
                }

                // for Parameter Bean
                final DfParameterBeanResolver resolver = createParameterBeanResolver();
                final DfPmbMetaData pmbMetaData = resolver.extractPmbMetaData(sql);
                if (pmbMetaData != null) {
                    if (customizeEntityInfo != null) {
                        pmbMetaData.setCustomizeEntityInfo(customizeEntityInfo);
                        customizeEntityInfo.setPmbMetaData(pmbMetaData); // reverse reference
                    }
                    final String pmbName = pmbMetaData.getClassName();
                    assertDuplicateParameterBean(pmbName, _sqlFile);
                    _sql2entityMeta.addPmbMetaData(pmbName, pmbMetaData);
                }
            }
        } catch (SQLException e) {
            if (_runInfo.isErrorContinue()) {
                _log.warn("Failed to execute: " + sql, e);
                _sql2entityMeta.addExceptionInfo(_sqlFile.getName(), e.getMessage() + ln() + sql);
                return;
            }
            throwSQLFailureException(sql, e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignored) {
                    _log.warn("Ignored exception: " + ignored.getMessage());
                }
            }
        }
    }

    protected Map<String, String> createColumnForcedJavaNativeMap(String sql) {
        final List<String> entityPropertyTypeList = getEntityPropertyTypeList(sql);
        final Map<String, String> columnJavaNativeMap = StringKeyMap.createAsFlexible();
        for (String element : entityPropertyTypeList) {
            final String nameDelimiter = " ";
            final int nameDelimiterLength = nameDelimiter.length();
            element = element.trim();
            final int nameIndex = element.lastIndexOf(nameDelimiter);
            if (nameIndex <= 0) {
                String msg = "The customize entity element should be [typeName columnName].";
                msg = msg + " But: element=" + element;
                msg = msg + " srcFile=" + _sqlFile;
                throw new IllegalStateException(msg);
            }
            final String typeName = resolvePackageName(element.substring(0, nameIndex).trim());
            final String columnName = element.substring(nameIndex + nameDelimiterLength).trim();
            columnJavaNativeMap.put(columnName, typeName);
        }
        return columnJavaNativeMap;
    }

    protected String resolvePackageName(String typeName) { // [DBFLUTE-271]
        return _propertyTypePackageResolver.resolvePackageName(typeName);
    }

    protected DfParameterBeanResolver createParameterBeanResolver() {
        return new DfParameterBeanResolver(_sql2entityMeta, _sqlFile, _schemaData);
    }

    // ===================================================================================
    //                                                                     CustomizeEntity
    //                                                                     ===============
    protected boolean isTargetEntityMakingSql(String sql) {
        final String entityName = getCustomizeEntityName(sql);
        if (entityName == null) {
            return false;
        }
        if ("df:x".equalsIgnoreCase(entityName)) { // non target making SQL!
            return false;
        }
        return true;
    }

    // ===================================================================================
    //                                                                       ParameterBean
    //                                                                       =============
    protected boolean isTargetParameterBeanMakingSql(String sql) {
        final String parameterBeanName = getParameterBeanName(sql);
        return parameterBeanName != null;
    }

    // ===================================================================================
    //                                                                 Override Adjustment
    //                                                                 ===================
    @Override
    protected String replaceCommentQuestionMarkIfNeeds(String line) {
        if (line.indexOf("--!!") >= 0 || line.indexOf("-- !!") >= 0) {
            // If the line comment is for a property of parameter-bean, 
            // it does not replace question mark.
            return line;
        }
        return super.replaceCommentQuestionMarkIfNeeds(line);
    }

    @Override
    protected boolean isTargetSql(String sql) {
        final String entityName = getCustomizeEntityName(sql);
        final String parameterBeanClassDefinition = getParameterBeanName(sql);

        // No Pmb and Non Target Entity --> Non Target
        if (parameterBeanClassDefinition == null && entityName != null && "df:x".equalsIgnoreCase(entityName)) {
            return false;
        }

        return entityName != null || parameterBeanClassDefinition != null;
    }

    @Override
    protected void traceSql(String sql) {
        _log.info("{SQL}" + ln() + sql);
    }

    @Override
    protected void traceResult(int goodSqlCount, int totalSqlCount) {
        if (totalSqlCount > 0) {
            _log.info("  --> success=" + goodSqlCount + " failure=" + (totalSqlCount - goodSqlCount));
        } else {
            _log.info("  --> SQL for sql2entity was not found in the SQL file!");
        }
    }

    @Override
    protected boolean isSqlTrimAndRemoveLineSeparator() {
        return false;
    }

    // ===================================================================================
    //                                                                   Assert Definition
    //                                                                   =================
    protected void assertDuplicateEntity(String entityName, File currentSqlFile) {
        final DfCustomizeEntityInfo entityInfo = _sql2entityMeta.getEntityInfoMap().get(entityName);
        if (entityInfo == null) {
            return;
        }
        final File sqlFile = entityInfo.getSqlFile();
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The same-name customize-entities were found.");
        br.addItem("CustomizeEntity");
        br.addElement(entityName);
        br.addItem("SQL Files");
        br.addElement(sqlFile);
        br.addElement(currentSqlFile);
        final String msg = br.buildExceptionMessage();
        throw new DfCustomizeEntityDuplicateException(msg);
    }

    protected void assertDuplicateParameterBean(String pmbName, File currentSqlFile) {
        final DfPmbMetaData metaData = _sql2entityMeta.getPmbMetaDataMap().get(pmbName);
        if (metaData == null) {
            return;
        }
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The same-name parameter-beans were found.");
        br.addItem("ParameterBean");
        br.addElement(pmbName);
        br.addItem("SQL Files");
        br.addElement(metaData.getSqlFile());
        br.addElement(currentSqlFile);
        final String msg = br.buildExceptionMessage();
        throw new DfParameterBeanDuplicateException(msg);
    }

    // ===================================================================================
    //                                                                           Analyzing
    //                                                                           =========
    protected String getCustomizeEntityName(final String sql) {
        return _outsideSqlMarkAnalyzer.getCustomizeEntityName(sql);
    }

    protected boolean isDomain(final String sql) {
        return _outsideSqlMarkAnalyzer.isDomain(sql);
    }

    protected boolean isCursor(final String sql) {
        return _outsideSqlMarkAnalyzer.isCursor(sql);
    }

    protected boolean isScalar(final String sql) {
        return _outsideSqlMarkAnalyzer.isScalar(sql);
    }

    protected List<String> getEntityPropertyTypeList(final String sql) {
        return _outsideSqlMarkAnalyzer.getCustomizeEntityPropertyTypeList(sql);
    }

    protected String getParameterBeanName(final String sql) {
        return _outsideSqlMarkAnalyzer.getParameterBeanName(sql);
    }

    protected List<String> getPrimaryKeyColumnNameList(final String sql) {
        return _outsideSqlMarkAnalyzer.getPrimaryKeyColumnNameList(sql);
    }

    protected String resolveEntityNameIfNeeds(String className, File file) {
        return _sqlFileNameResolver.resolveEntityNameIfNeeds(className, file.getName());
    }

    // ===================================================================================
    //                                                                          SQL Helper
    //                                                                          ==========
    protected String removeBlockComment(final String sql) {
        return Srl.removeBlockComment(sql);
    }

    protected String removeLineComment(final String sql) {
        return Srl.removeLineComment(sql); // with removing CR
    }

    // ===================================================================================
    //                                                                          Properties
    //                                                                          ==========
    protected DfBuildProperties getProperties() {
        return DfBuildProperties.getInstance();
    }

    protected DBDef currentDBDef() {
        return getBasicProperties().getCurrentDBDef();
    }

    protected DfBasicProperties getBasicProperties() {
        return getProperties().getBasicProperties();
    }

    protected DfDatabaseProperties getDatabaseProperties() {
        return getProperties().getDatabaseProperties();
    }
}