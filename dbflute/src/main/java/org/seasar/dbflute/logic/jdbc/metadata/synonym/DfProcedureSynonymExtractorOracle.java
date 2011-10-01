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
package org.seasar.dbflute.logic.jdbc.metadata.synonym;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.engine.database.model.UnifiedSchema;
import org.seasar.dbflute.exception.SQLFailureException;
import org.seasar.dbflute.helper.StringKeyMap;
import org.seasar.dbflute.helper.jdbc.facade.DfJdbcFacade;
import org.seasar.dbflute.logic.jdbc.metadata.DfAbstractMetaDataExtractor;
import org.seasar.dbflute.logic.jdbc.metadata.basic.DfProcedureExtractor;
import org.seasar.dbflute.logic.jdbc.metadata.info.DfProcedureColumnMeta;
import org.seasar.dbflute.logic.jdbc.metadata.info.DfProcedureMeta;
import org.seasar.dbflute.logic.jdbc.metadata.info.DfProcedureMeta.DfProcedureType;
import org.seasar.dbflute.logic.jdbc.metadata.info.DfProcedureSynonymMeta;
import org.seasar.dbflute.logic.jdbc.metadata.info.DfSynonymMeta;
import org.seasar.dbflute.logic.jdbc.metadata.procedure.DfProcedureNativeExtractorOracle;
import org.seasar.dbflute.logic.jdbc.metadata.procedure.DfProcedureNativeExtractorOracle.ProcedureNativeInfo;
import org.seasar.dbflute.logic.jdbc.metadata.procedure.DfProcedureParameterExtractorOracle.ProcedureArgumentInfo;
import org.seasar.dbflute.logic.jdbc.metadata.synonym.DfDBLinkExtractorOracle.DBLinkInfo;
import org.seasar.dbflute.util.DfCollectionUtil;

/**
 * @author jflute
 * @since 0.9.6.2 (2009/12/08 Tuesday)
 */
public class DfProcedureSynonymExtractorOracle extends DfAbstractMetaDataExtractor implements
        DfProcedureSynonymExtractor {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Log _log = LogFactory.getLog(DfProcedureSynonymExtractorOracle.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected DataSource _dataSource;
    protected List<UnifiedSchema> _targetSchemaList;

    // ===================================================================================
    //                                                                             Extract
    //                                                                             =======
    public Map<String, DfProcedureSynonymMeta> extractProcedureSynonymMap() {
        _log.info("...Extracting procedure synonym");
        final Map<String, DfProcedureSynonymMeta> procedureSynonymMap = StringKeyMap.createAsFlexibleOrdered();
        final String sql = buildSynonymSelect();
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = _dataSource.getConnection();
            final DatabaseMetaData metaData = conn.getMetaData();
            final Map<String, DfProcedureMeta> procedureMap = new LinkedHashMap<String, DfProcedureMeta>();
            final List<DfProcedureMeta> procedureList = new ArrayList<DfProcedureMeta>();
            final DfProcedureExtractor procedureExtractor = new DfProcedureExtractor();
            procedureExtractor.suppressLogging();
            for (UnifiedSchema unifiedSchema : _targetSchemaList) {
                // get new procedure list because different instances is needed at this process
                procedureList.addAll(procedureExtractor.getPlainProcedureList(_dataSource, metaData, unifiedSchema));
            }
            for (DfProcedureMeta metaInfo : procedureList) {
                final String procedureKeyName = metaInfo.getProcedureFullQualifiedName();
                procedureMap.put(procedureKeyName, metaInfo);
            }
            Map<String, Map<String, ProcedureNativeInfo>> dbLinkProcedureNativeMap = null;
            st = conn.createStatement();
            _log.info(sql);
            rs = st.executeQuery(sql);
            while (rs.next()) {
                final UnifiedSchema synonymOwner = createAsDynamicSchema(null, rs.getString("OWNER"));
                final String synonymName = rs.getString("SYNONYM_NAME");
                final UnifiedSchema tableOwner = createAsDynamicSchema(null, rs.getString("TABLE_OWNER"));
                final String tableName = rs.getString("TABLE_NAME");
                final String dbLinkName = rs.getString("DB_LINK");

                final DfSynonymMeta synonymMetaInfo = new DfSynonymMeta();

                // Basic
                synonymMetaInfo.setSynonymOwner(synonymOwner);
                synonymMetaInfo.setSynonymName(synonymName);
                synonymMetaInfo.setTableOwner(tableOwner);
                synonymMetaInfo.setTableName(tableName);
                synonymMetaInfo.setDBLinkName(dbLinkName);

                // Select-able?
                judgeSynonymSelectable(synonymMetaInfo);

                if (synonymMetaInfo.isSelectable()) {
                    continue; // select-able synonyms are out of target
                }
                final DfProcedureMeta procedureMeta;
                if (dbLinkName != null && dbLinkName.trim().length() > 0) {
                    if (dbLinkProcedureNativeMap == null) { // lazy load
                        dbLinkProcedureNativeMap = extractDBLinkProcedureNativeMap();
                    }
                    procedureMeta = prepareDBLinkProcedureNative(tableName, dbLinkName, dbLinkProcedureNativeMap);
                    if (procedureMeta == null) {
                        continue;
                    }
                } else {
                    final String procedureKey = tableOwner.buildSchemaQualifiedName(tableName);
                    procedureMeta = procedureMap.get(procedureKey);
                    if (procedureMeta == null) {
                        // Synonym for Package Procedure has several problems.
                        //  o Synonym meta data does not have its package info (needs to trace more)
                        //  o Oracle cannot execute Synonym for Package Procedure *fundamental problem
                        // So it is not supported here.
                        //for (String schemaName : _schemaList) {
                        //    procedureMetaInfo = procedureMap.get(schemaName + "." + procedureKey);
                        //    if (procedureMetaInfo != null) {
                        //        break; // comes first  
                        //    }
                        //}
                        //if (procedureMetaInfo == null) {
                        continue;
                        //}
                    }
                }
                procedureMeta.setProcedureSynonym(true);
                final DfProcedureSynonymMeta procedureSynonymMetaInfo = new DfProcedureSynonymMeta();
                procedureSynonymMetaInfo.setProcedureMetaInfo(procedureMeta);
                procedureSynonymMetaInfo.setSynonymMetaInfo(synonymMetaInfo);
                final String synonymKey = buildSynonymMapKey(synonymOwner, synonymName);
                procedureSynonymMap.put(synonymKey, procedureSynonymMetaInfo);
            }
        } catch (SQLException e) {
            String msg = "Failed to get procedure synonyms: sql=" + sql;
            throw new SQLFailureException(msg, e);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ignored) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignored) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return procedureSynonymMap;
    }

    protected String buildSynonymSelect() {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (UnifiedSchema unifiedSchema : _targetSchemaList) {
            if (count > 0) {
                sb.append(", ");
            }
            sb.append("'").append(unifiedSchema.getPureSchema()).append("'");
            ++count;
        }
        final String sql = "select * from ALL_SYNONYMS where OWNER in (" + sb.toString() + ")";
        return sql;
    }

    protected String buildSynonymMapKey(UnifiedSchema synonymOwner, String synonymName) {
        return synonymOwner.buildSchemaQualifiedName(synonymName);
    }

    protected void judgeSynonymSelectable(DfSynonymMeta info) {
        final DfJdbcFacade facade = new DfJdbcFacade(_dataSource);
        final String synonymSqlName = info.buildSynonymSqlName();
        final String sql = "select * from " + synonymSqlName + " where 0 = 1";
        try {
            final List<String> columnList = new ArrayList<String>();
            columnList.add("dummy");
            facade.selectStringList(sql, columnList);
            info.setSelectable(true);
        } catch (RuntimeException ignored) {
            info.setSelectable(false);
        }
    }

    protected Map<String, Map<String, ProcedureNativeInfo>> extractDBLinkProcedureNativeMap() {
        final boolean logging = false;
        final DfDBLinkExtractorOracle dbLinkExtractor = new DfDBLinkExtractorOracle(_dataSource, logging);
        final Map<String, DBLinkInfo> dbLinkInfoMap = dbLinkExtractor.selectDBLinkInfoMap(); // main schema's only
        final DfProcedureNativeExtractorOracle nativeExtractor = new DfProcedureNativeExtractorOracle(_dataSource,
                logging);
        final Map<String, Map<String, ProcedureNativeInfo>> map = DfCollectionUtil.newLinkedHashMap();
        for (String dbLinkName : dbLinkInfoMap.keySet()) {
            map.put(dbLinkName, nativeExtractor.extractDBLinkProcedureNativeInfoList(dbLinkName));
        }
        return map;
    }

    protected DfProcedureMeta prepareDBLinkProcedureNative(String tableName, String dbLinkName,
            Map<String, Map<String, ProcedureNativeInfo>> dbLinkProcedureNativeMap) {
        final Map<String, ProcedureNativeInfo> nativeMap = dbLinkProcedureNativeMap.get(dbLinkName);
        if (nativeMap == null) {
            return null; // it might be next schema DB link
        }
        // Synonym for Package Procedure has several problems.
        // So it is not supported here.
        final String nativeInfoMapKey = generateNativeInfoMapKey(null, tableName, null);
        final ProcedureNativeInfo nativeInfo = nativeMap.get(nativeInfoMapKey);
        if (nativeInfo == null) {
            return null; // it might be package procedures
        }
        final DfProcedureMeta procedureMeta = new DfProcedureMeta();
        procedureMeta.setProcedureCatalog(nativeInfo.getObjectName());
        procedureMeta.setProcedureName(nativeInfo.getProcedureName());
        final String linkedName = nativeInfo.getProcedureName() + "@" + dbLinkName;
        procedureMeta.setProcedureFullQualifiedName(linkedName);
        procedureMeta.setProcedureSqlName(linkedName);
        procedureMeta.setProcedureType(DfProcedureType.procedureResultUnknown);
        final List<ProcedureArgumentInfo> argInfoList = nativeInfo.getArgInfoList();
        for (ProcedureArgumentInfo argInfo : argInfoList) {
            // TODO impl
            final DfProcedureColumnMeta columnMeta = new DfProcedureColumnMeta();
            columnMeta.setColumnName(argInfo.getArgumentName());
        }
        return procedureMeta;
    }

    protected String generateNativeInfoMapKey(String packageName, String procedureName, String overload) {
        return DfProcedureNativeExtractorOracle.generateNativeInfoMapKey(packageName, procedureName, overload);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public void setDataSource(DataSource dataSource) {
        _dataSource = dataSource;
    }

    public void setTargetSchemaList(List<UnifiedSchema> targetSchemaList) {
        this._targetSchemaList = targetSchemaList;
    }
}
