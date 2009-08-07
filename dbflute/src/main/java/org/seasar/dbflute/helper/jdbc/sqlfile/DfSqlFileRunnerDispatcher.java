package org.seasar.dbflute.helper.jdbc.sqlfile;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author jflute
 * @since 0.9.5.4 (2009/08/07 Friday)
 */
public interface DfSqlFileRunnerDispatcher {

    /**
     * Dispatch executing a SQL.
     * @param sqlFile The SQL file that contains the SQL. (NotNull)
     * @param stmt Statement. (NotNull)
     * @param sql SQL string. (NotNull)
     * @return Is the dispatching success?
     * @throws SQLException
     */
    boolean dispatch(File sqlFile, Statement stmt, String sql) throws SQLException;
}
