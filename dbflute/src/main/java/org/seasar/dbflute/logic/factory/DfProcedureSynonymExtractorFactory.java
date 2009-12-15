package org.seasar.dbflute.logic.factory;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.seasar.dbflute.logic.jdbc.metadata.synonym.DfProcedureSynonymExtractor;
import org.seasar.dbflute.logic.jdbc.metadata.synonym.DfProcedureSynonymExtractorOracle;
import org.seasar.dbflute.properties.DfBasicProperties;
import org.seasar.dbflute.properties.DfDatabaseProperties;

/**
 * @author jflute
 * @since 0.9.6.2 (2009/12/08 Tuesday)
 */
public class DfProcedureSynonymExtractorFactory {

    protected DataSource _dataSource;
    protected DfBasicProperties _basicProperties;
    protected DfDatabaseProperties _databaseProperties;

    /**
     * @param dataSource The data source. (NotNull)
     * @param basicProperties The basic properties. (NotNull)
     * @param databaseProperties The database properties. (NotNull)
     */
    public DfProcedureSynonymExtractorFactory(DataSource dataSource, DfBasicProperties basicProperties,
            DfDatabaseProperties databaseProperties) {
        _dataSource = dataSource;
        _basicProperties = basicProperties;
        _databaseProperties = databaseProperties;
    }

    /**
     * @return The extractor of DB comments. (Nullable)
     */
    public DfProcedureSynonymExtractor createSynonymExtractor() {
        if (_basicProperties.isDatabaseOracle()) {
            final DfProcedureSynonymExtractorOracle extractor = new DfProcedureSynonymExtractorOracle();
            extractor.setDataSource(_dataSource);
            extractor.setAllSchemaList(createAllSchemaList());
            return extractor;
        }
        return null;
    }

    protected List<String> createAllSchemaList() { // not only main schema but also additional schemas
        final List<String> schemaList = new ArrayList<String>();
        schemaList.add(_databaseProperties.getDatabaseSchema());
        schemaList.addAll(_databaseProperties.getAdditionalSchemaMap().keySet());
        return schemaList;
    }
}