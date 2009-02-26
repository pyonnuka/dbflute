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
package org.seasar.dbflute.helper.jdbc.metadata.synonym;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.seasar.dbflute.helper.jdbc.metadata.DfAutoIncrementHandler;
import org.seasar.dbflute.helper.jdbc.metadata.DfForeignKeyHandler;
import org.seasar.dbflute.helper.jdbc.metadata.DfIndexHandler;
import org.seasar.dbflute.helper.jdbc.metadata.DfTableHandler;
import org.seasar.dbflute.helper.jdbc.metadata.DfUniqueKeyHandler;
import org.seasar.dbflute.helper.jdbc.metadata.info.DfForeignKeyMetaInfo;
import org.seasar.dbflute.helper.jdbc.metadata.info.DfSynonymMetaInfo;
import org.seasar.dbflute.helper.jdbc.metadata.info.DfTableMetaInfo;

/**
 * @author jflute
 * @since 0.9.3 (2009/02/24 Tuesday)
 */
public class DfSynonymExtractorOracle implements DfSynonymExtractor {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Log _log = LogFactory.getLog(DfSynonymExtractorOracle.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected DataSource _dataSource;
    protected DfTableHandler _tableHandler = new DfTableHandler();
    protected DfUniqueKeyHandler _uniqueKeyHandler = new DfUniqueKeyHandler();
    protected DfAutoIncrementHandler _autoIncrementHandler = new DfAutoIncrementHandler();
    protected DfForeignKeyHandler _foreignKeyHandler = new DfForeignKeyHandler() {
        @Override
        public boolean isTableExcept(String tableName) {
            // All foreign tables are target if the foreign table is except.
            // Because the filtering is executed when translating foreign keys.
            return false;
        }
    };
    protected DfIndexHandler _indexHandler = new DfIndexHandler();

    // ===================================================================================
    //                                                                                Main
    //                                                                                ====
    public Map<String, DfSynonymMetaInfo> extractSynonymMap() {
        final Connection conn;
        try {
            conn = _dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        try {
            final Map<String, DfSynonymMetaInfo> synonymMap = new LinkedHashMap<String, DfSynonymMetaInfo>();
            final Statement statement = conn.createStatement();
            final ResultSet rs = statement.executeQuery("select * from USER_SYNONYMS");
            while (rs.next()) {
                String synonymName = rs.getString("SYNONYM_NAME");
                String tableOwner = rs.getString("TABLE_OWNER");
                String tableName = rs.getString("TABLE_NAME");

                if (_tableHandler.isTableExcept(synonymName)) {
                    continue;
                }
                if (tableOwner == null || tableOwner.trim().length() == 0) {
                    // It is possible that the synonym is for database link.
                    // Now a synonym is for database link is unsupported here.
                    continue;
                }

                final DfSynonymMetaInfo info = new DfSynonymMetaInfo();

                // Basic
                info.setSynonymName(synonymName);
                info.setTableOwner(tableOwner);
                info.setTableName(tableName);

                // PK, ID, UQ, FK, Index
                try {
                    final DatabaseMetaData metaData = conn.getMetaData();
                    info.setPrimaryKeyNameList(getPKList(metaData, tableOwner, tableName));
                    final List<String> primaryKeyNameList = info.getPrimaryKeyNameList();
                    for (String primaryKeyName : primaryKeyNameList) {
                        final boolean autoIncrement = isAutoIncrement(conn, tableOwner, tableName, primaryKeyName);
                        if (autoIncrement) {
                            info.setAutoIncrement(autoIncrement);
                            break;
                        }
                    }
                    info.setUniqueKeyMap(getUQMap(metaData, tableOwner, tableName, primaryKeyNameList));
                    info.setForeignKeyMetaInfoMap(getFKMap(metaData, tableOwner, tableName)); // It's tentative information at this timing!
                    final Map<String, Map<Integer, String>> uniqueKeyMap = info.getUniqueKeyMap();
                    info.setIndexMap(_indexHandler.getIndexMap(metaData, tableOwner, tableName, uniqueKeyMap));
                } catch (Exception continued) {
                    _log.info("Failed to get meta data of " + synonymName + ": " + continued.getMessage());
                    continue;
                }
                synonymMap.put(synonymName, info);
            }
            translateFKTable(synonymMap); // It translates foreign key meta informations. 
            return synonymMap;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    protected List<String> getPKList(DatabaseMetaData metaData, String tableOwner, String tableName) {
        try {
            return _uniqueKeyHandler.getPrimaryColumnNameList(metaData, tableOwner, tableName);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Map<String, Map<Integer, String>> getUQMap(DatabaseMetaData metaData, String tableOwner,
            String tableName, List<String> primaryKeyNameList) {
        try {
            return _uniqueKeyHandler.getUniqueKeyMap(metaData, tableOwner, tableName, primaryKeyNameList);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Map<String, DfForeignKeyMetaInfo> getFKMap(DatabaseMetaData metaData, String tableOwner, String tableName) {
        try {
            return _foreignKeyHandler.getForeignKeyMetaInfo(metaData, tableOwner, tableName);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected boolean isAutoIncrement(Connection conn, String tableOwner, String tableName, String primaryKeyColumnName) {
        try {
            final DfTableMetaInfo tableMetaInfo = new DfTableMetaInfo();
            tableMetaInfo.setTableName(tableName);
            tableMetaInfo.setTableSchema(tableOwner);
            return _autoIncrementHandler.isAutoIncrementColumn(conn, tableMetaInfo, primaryKeyColumnName);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void translateFKTable(Map<String, DfSynonymMetaInfo> synonymMap) {
        final Collection<DfSynonymMetaInfo> synonymList = synonymMap.values();
        final Map<String, List<String>> tableForeignSynonymListMap = new LinkedHashMap<String, List<String>>();
        for (DfSynonymMetaInfo synonym : synonymList) {
            final String synonymName = synonym.getSynonymName();
            final String tableName = synonym.getTableName();
            final List<String> foreignSynonymList = tableForeignSynonymListMap.get(tableName);
            if (foreignSynonymList != null) {
                foreignSynonymList.add(synonymName);
            } else {
                final List<String> foreignNewSynonymList = new ArrayList<String>();
                foreignNewSynonymList.add(synonymName);
                tableForeignSynonymListMap.put(tableName, foreignNewSynonymList);
            }
        }
        for (DfSynonymMetaInfo synonym : synonymList) {
            final Map<String, DfForeignKeyMetaInfo> fkMap = synonym.getForeignKeyMetaInfoMap();
            if (fkMap == null || fkMap.isEmpty()) {
                continue;
            }
            final Set<String> fkNameSet = fkMap.keySet();
            final Map<String, DfForeignKeyMetaInfo> additionalFKMap = new LinkedHashMap<String, DfForeignKeyMetaInfo>();
            final List<String> removedFKKeyList = new ArrayList<String>();
            for (String fkName : fkNameSet) {
                final DfForeignKeyMetaInfo fk = fkMap.get(fkName);

                // - - - - - - - - - - - - - - -
                // Translate a local table name.
                // - - - - - - - - - - - - - - -
                fk.setLocalTableName(synonym.getSynonymName());

                // - - - - - - - - - - - - - - - -
                // Translate a foreign table name.
                // - - - - - - - - - - - - - - - -
                final String foreignTableName = fk.getForeignTableName();

                final List<String> foreignSynonymList = tableForeignSynonymListMap.get(foreignTableName);
                if (foreignSynonymList == null || foreignSynonymList.isEmpty()) {
                    if (_tableHandler.isTableExcept(foreignTableName)) {
                        removedFKKeyList.add(foreignTableName);
                    }
                    continue;
                }
                boolean firstDone = false;
                for (int i = 0; i < foreignSynonymList.size(); i++) {
                    final String foreignSynonymName = foreignSynonymList.get(i);

                    if (!firstDone) {
                        fk.setForeignTableName(foreignSynonymName);
                        firstDone = true;
                        continue;
                    }

                    final DfForeignKeyMetaInfo additionalFK = new DfForeignKeyMetaInfo();
                    additionalFK.setForeignKeyName(fk.getForeignKeyName() + "_" + (i + 1));
                    additionalFK.setLocalTableName(synonym.getSynonymName());
                    additionalFK.setForeignTableName(foreignSynonymName);
                    additionalFK.setColumnNameMap(fk.getColumnNameMap());
                    additionalFKMap.put(additionalFK.getForeignKeyName(), additionalFK);
                }
            }
            fkMap.putAll(additionalFKMap);
            for (String removedKey : removedFKKeyList) {
                fkMap.remove(removedKey);
            }
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public void setDataSource(DataSource dataSource) {
        _dataSource = dataSource;
    }
}
