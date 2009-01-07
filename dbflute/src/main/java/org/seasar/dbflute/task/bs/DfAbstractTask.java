/*
 * Copyright 2004-2006 the Seasar Foundation and the Others.
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
package org.seasar.dbflute.task.bs;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Task;
import org.seasar.dbflute.DfBuildProperties;
import org.seasar.dbflute.config.DfEnvironmentType;
import org.seasar.dbflute.helper.jdbc.connection.DfSimpleDataSourceCreator;
import org.seasar.dbflute.helper.jdbc.context.DfDataSourceContext;
import org.seasar.dbflute.logic.sqlfile.SqlFileCollector;
import org.seasar.dbflute.properties.DfBasicProperties;
import org.seasar.dbflute.properties.DfDatabaseInfoProperties;
import org.seasar.dbflute.torque.DfAntTaskUtil;

/**
 * The abstract task.
 * @author jflute
 */
public abstract class DfAbstractTask extends Task {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Log _log = LogFactory.getLog(DfAbstractTask.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** DB driver. */
    protected String _driver;

    /** DB URL. */
    protected String _url;

    /** Schema name. */
    protected String _schema;

    /** User name. */
    protected String _userId;

    /** Password */
    protected String _password;

    /** Connection properties. */
    protected Properties _connectionProperties;

    protected DfSimpleDataSourceCreator _dataSourceCreator = new DfSimpleDataSourceCreator();

    // ===================================================================================
    //                                                                             Execute
    //                                                                             =======
    @Override
    final public void execute() {
        long before = getTaskBeforeTimeMillis();
        try {
            initializeDatabaseInfo();
            if (isUseDataSource()) {
                setupDataSource();
                connectSchema();
            }
            doExecute();
            if (isUseDataSource()) {
                closingDataSource();
            }
        } catch (RuntimeException e) {
            try {
                logRuntimeException(e);
            } catch (RuntimeException ignored) {
                _log.warn("Ignored exception occured!", ignored);
                _log.error("Failed to execute DBFlute Task!", e);
            }
            throw e;
        } catch (Error e) {
            try {
                logError(e);
            } catch (RuntimeException ignored) {
                _log.warn("Ignored exception occured!", ignored);
                _log.error("Failed to execute DBFlute Task!", e);
            }
            throw e;
        } finally {
            long after = getTaskAfterTimeMillis();
            if (isValidTaskEndInformation()) {
                _log.info("");
                _log.info("_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/ {Task End}");
                _log.info("[" + getDisplayTaskName() + "]: " + getPerformanceView(after - before));
                _log.info("");
                _log.info("  MY_PROJECT_NAME: {" + getBasicProperties().getProjectName() + "}");
                _log.info("    database  = " + getBasicProperties().getDatabaseName());
                _log.info("    language  = " + getBasicProperties().getTargetLanguage());
                _log.info("    container = " + getBasicProperties().getTargetContainerName());
                _log.info("");
                _log.info("  DBFLUTE_ENVIRONMENT_TYPE: {" + DfEnvironmentType.getInstance().getEnvironmentType() + "}");
                _log.info("    driver = " + _driver);
                _log.info("    url    = " + _url);
                _log.info("    schema = " + _schema);
                _log.info("    user   = " + _userId);
                _log.info("    props  = " + _connectionProperties);
                _log.info("_/_/_/_/_/_/_/_/_/_/");
            }
            _log.info("");
        }
    }

    protected long getTaskBeforeTimeMillis() {
        return System.currentTimeMillis();
    }

    protected long getTaskAfterTimeMillis() {
        return System.currentTimeMillis();
    }

    protected boolean isValidTaskEndInformation() {
        return true;
    }

    protected void logRuntimeException(RuntimeException e) {
        DfAntTaskUtil.logRuntimeException(e, getDisplayTaskName());
    }

    protected void logError(Error e) {
        DfAntTaskUtil.logError(e, getDisplayTaskName());
    }

    protected String getDisplayTaskName() {
        final String taskName = getTaskName();
        return DfAntTaskUtil.getDisplayTaskName(taskName);
    }

    protected void initializeDatabaseInfo() {
        _driver = getDatabaseInfoProperties().getDatabaseDriver();
        _url = getDatabaseInfoProperties().getDatabaseUri();
        _userId = getDatabaseInfoProperties().getDatabaseUser();
        _schema = getDatabaseInfoProperties().getDatabaseSchema();
        _password = getDatabaseInfoProperties().getDatabasePassword();
        _connectionProperties = getDatabaseInfoProperties().getDatabaseConnectionProperties();
    }

    abstract protected void doExecute();

    /**
     * Get performance view.
     * @param mil The value of millisecond.
     * @return Performance view. (ex. 1m23s456ms) (NotNull)
     */
    protected String getPerformanceView(long mil) {
        if (mil < 0) {
            return String.valueOf(mil);
        }

        long sec = mil / 1000;
        long min = sec / 60;
        sec = sec % 60;
        mil = mil % 1000;

        StringBuffer sb = new StringBuffer();
        if (min >= 10) { // Minute
            sb.append(min).append("m");
        } else if (min < 10 && min >= 0) {
            sb.append("0").append(min).append("m");
        }
        if (sec >= 10) { // Second
            sb.append(sec).append("s");
        } else if (sec < 10 && sec >= 0) {
            sb.append("0").append(sec).append("s");
        }
        if (mil >= 100) { // Millisecond
            sb.append(mil).append("ms");
        } else if (mil < 100 && mil >= 10) {
            sb.append("0").append(mil).append("ms");
        } else if (mil < 10 && mil >= 0) {
            sb.append("00").append(mil).append("ms");
        }

        return sb.toString();
    }

    protected String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    abstract protected boolean isUseDataSource();

    protected void setupDataSource() {
        _dataSourceCreator.setUserId(_userId);
        _dataSourceCreator.setPassword(_password);
        _dataSourceCreator.setDriver(_driver);
        _dataSourceCreator.setUrl(_url);
        _dataSourceCreator.setConnectionProperties(_connectionProperties);
        _dataSourceCreator.setAutoCommit(true);
        _dataSourceCreator.create();
    }

    protected void closingDataSource() {
        _dataSourceCreator.commit();
        _dataSourceCreator.destroy();
    }

    protected DataSource getDataSource() {
        return DfDataSourceContext.getDataSource();
    }

    protected void connectSchema() {
        if (getBasicProperties().isDatabaseDB2() && _schema != null) {
            final Statement statement;
            try {
                statement = getDataSource().getConnection().createStatement();
            } catch (SQLException e) {
                _log.warn("Connection#createStatement() threw the SQLException: " + e.getMessage());
                return;
            }
            final String sql = "SET CURRENT SCHEMA = " + _schema.trim();
            try {
                _log.info("...Executing command: " + sql);
                statement.execute(sql);
            } catch (SQLException e) {
                _log.warn("'" + sql + "' threw the SQLException: " + e.getMessage());
                return;
            }
        }
    }

    public void setContextProperties(String file) {
        final Properties prop = DfAntTaskUtil.getBuildProperties(file, super.project);
        DfBuildProperties.getInstance().setProperties(prop);
    }

    protected DfBuildProperties getProperties() {
        return DfBuildProperties.getInstance();
    }

    protected DfBasicProperties getBasicProperties() {
        return getProperties().getBasicProperties();
    }

    protected DfDatabaseInfoProperties getDatabaseInfoProperties() {
        return getProperties().getDatabaseInfoProperties();
    }

    // ===================================================================================
    //                                                                 SQL File Collecting
    //                                                                 ===================
    /**
     * Collect SQL files the list.
     * @return The list of SQL files. (NotNull)
     */
    protected List<File> collectSqlFileList() {
        final String sqlDirectory = getProperties().getOutsideSqlProperties().getSqlDirectory();
        final SqlFileCollector sqlFileCollector = new SqlFileCollector(sqlDirectory, getBasicProperties());
        return sqlFileCollector.collectSqlFileList();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public void setEnvironmentType(String environmentType) {
        DfEnvironmentType.getInstance().setEnvironmentType(environmentType);
    }
}