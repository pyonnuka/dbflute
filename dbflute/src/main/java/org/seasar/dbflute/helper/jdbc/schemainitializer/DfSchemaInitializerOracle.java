package org.seasar.dbflute.helper.jdbc.schemainitializer;

import javax.sql.DataSource;

/**
 * 
 * @author jflute
 */
public class DfSchemaInitializerOracle implements DfSchemaInitializer {

    protected DfSchemaInitializerJdbc _schemaInitializer = new DfSchemaInitializerJdbc();

    public void setDataSource(DataSource dataSource) {
        _schemaInitializer.setDataSource(dataSource);
    }

    public void initializeSchema() {
        _schemaInitializer.initializeSchema();
    }

    // * * * * * * * * * * * * * * * * * * * * * * * *  
    // USER_TABLES.STATUSが「Oracle 10g」からなので、
    // VERSION依存してしまわないようにした。
    // * * * * * * * * * * * * * * * * * * * * * * * * 
    //    protected DataSource _dataSource;
    //
    //    public void setDataSource(DataSource dataSource) {
    //        _dataSource = dataSource;
    //    }
    //
    //    public void initializeSchema() {
    //        dropForeignKey();
    //        dropTable();
    //    }
    //
    //    protected void dropForeignKey() {
    //        final DfGeneratedSqlExecutor generatedSqlExecutor = createGeneratedSqlExecutor();
    //        generatedSqlExecutor.execute(getDropForeignKeySql(), "sql");
    //    }
    //
    //    protected void dropTable() {
    //        final DfGeneratedSqlExecutor generatedSqlExecutor = createGeneratedSqlExecutor();
    //        generatedSqlExecutor.execute(getDropTableSql(), "sql");
    //    }
    //
    //    protected DfGeneratedSqlExecutor createGeneratedSqlExecutor() {
    //        final DfGeneratedSqlExecutorImpl generatedSqlExecutorImpl = new DfGeneratedSqlExecutorImpl();
    //        generatedSqlExecutorImpl.setDataSource(_dataSource);
    //        return generatedSqlExecutorImpl;
    //    }
    //
    //    protected String getDropForeignKeySql() {
    //        final String lineSeparator = System.getProperty("line.separator");
    //        final StringBuilder sb = new StringBuilder();
    //        sb.append("select 'ALTER TABLE ' || TABLE_NAME || ' DROP FOREIGN KEY ' || CONSTRAINT_NAME as sql");
    //        sb.append(lineSeparator);
    //        sb.append("  from USER_CONSTRAINTS");
    //        sb.append(lineSeparator);
    //        sb.append(" where CONSTRAINT_TYPE = 'R'");
    //        sb.append(lineSeparator);
    //        return sb.toString();
    //    }
    //
    //    protected String getDropTableSql() {
    //        final String lineSeparator = System.getProperty("line.separator");
    //        final StringBuilder sb = new StringBuilder();
    //        sb.append("select 'DROP TABLE ' || TABLE_NAME as sql");
    //        sb.append(lineSeparator);
    //        sb.append("  from USER_TABLES");
    //        sb.append(lineSeparator);
    //        sb.append(" where STATUS = 'VALID'");
    //        sb.append(lineSeparator);
    //        return sb.toString();
    //    }
}