/*
 * Copyright 2004-2007 the Seasar Foundation and the Others.
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.BuildException;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.texen.ant.TexenTask;
import org.seasar.dbflute.DfBuildProperties;
import org.seasar.dbflute.config.DfEnvironmentType;
import org.seasar.dbflute.helper.jdbc.connection.DfSimpleDataSourceCreator;
import org.seasar.dbflute.helper.jdbc.context.DfDataSourceContext;
import org.seasar.dbflute.logic.sqlfile.SqlFileCollector;
import org.seasar.dbflute.properties.DfBasicProperties;
import org.seasar.dbflute.properties.DfDatabaseInfoProperties;
import org.seasar.dbflute.properties.DfRefreshProperties;
import org.seasar.dbflute.torque.DfAntTaskUtil;
import org.seasar.dbflute.util.basic.DfStringUtil;
import org.seasar.dbflute.velocity.DfGenerator;

/**
 * The abstract class of texen task.
 * @author jflute
 */
public abstract class DfAbstractTexenTask extends TexenTask {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** Log instance. */
    public static final Log _log = LogFactory.getLog(DfAbstractTexenTask.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** Target database name. */
    protected String _targetDatabase;

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

    /** Data source creator. (for help) */
    protected DfSimpleDataSourceCreator _dataSourceCreator = new DfSimpleDataSourceCreator();

    // ===================================================================================
    //                                                                                Main
    //                                                                                ====
    // -----------------------------------------------------
    //                                               Execute
    //                                               -------
    @Override
    /**
     * The override.
     */
    final public void execute() {
        long before = System.currentTimeMillis();
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
            long after = System.currentTimeMillis();
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
            _log.info("");
        }
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

    protected void fireSuperExecute() {
        // /----------------------------------------------
        // Set up the encoding of templates from property.
        // -----/
        setInputEncoding(getBasicProperties().getTemplateFileEncoding());
        setOutputEncoding(getBasicProperties().getSourceFileEncoding());
        doExecuteAlmostSameAsSuper();
    }

    // -----------------------------------
    //                  Copy from Velocity
    //                  ------------------
    // Copy from super.execute() and Modify a little.
    private void doExecuteAlmostSameAsSuper() {
        if (templatePath == null && !useClasspath) {
            throw new IllegalStateException(
                    "The template path needs to be defined if you are not using the classpath for locating templates!");
        }
        if (controlTemplate == null) {
            throw new IllegalStateException("The control template needs to be defined!");
        }
        if (outputDirectory == null) {
            throw new IllegalStateException("The output directory needs to be defined!");
        }
        if (outputFile == null) {
            throw new IllegalStateException("The output file needs to be defined!");
        }
        try {
            if (templatePath != null) {
                log("Using templatePath: " + templatePath, 3);
                Velocity.setProperty("file.resource.loader.path", templatePath);
            }
            if (useClasspath) {
                log("Using classpath");
                Velocity.addProperty("resource.loader", "classpath");
                Velocity.setProperty("classpath.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
                Velocity.setProperty("classpath.resource.loader.cache", "false");
                Velocity.setProperty("classpath.resource.loader.modificationCheckInterval", "2");
            }
            Velocity.setProperty(VelocityEngine.RUNTIME_LOG, "./log/velocity.log");
            Velocity.init();
            final DfGenerator generator = getGeneratorHandler();
            generator.setOutputPath(outputDirectory);
            generator.setInputEncoding(inputEncoding);
            generator.setOutputEncoding(outputEncoding);
            if (templatePath != null) {
                generator.setTemplatePath(templatePath);
            }

            // - - - - - - - - - - - - - - - - - - - -
            // Remove writing output file of velocity.
            // - - - - - - - - - - - - - - - - - - - -
            // final File file = new File(outputDirectory);
            // if (!file.exists()) {
            //     file.mkdirs();
            // }
            // String path = outputDirectory + File.separator + outputFile;
            // log("Generating to file " + path, 2);
            // Writer writer = generator.getWriter(path, outputEncoding);

            Context c = initControlContext();
            populateInitialContext(c);
            if (contextProperties != null) {
                for (Iterator<?> i = contextProperties.getKeys(); i.hasNext();) {
                    String property = (String) i.next();
                    String value = contextProperties.getString(property);
                    try {
                        c.put(property, new Integer(value));
                    } catch (NumberFormatException nfe) {
                        String booleanString = contextProperties.testBoolean(value);
                        if (booleanString != null) {
                            c.put(property, new Boolean(booleanString));
                        } else {
                            if (property.endsWith("file.contents")) {
                                value = fileContentsToString(super.project.resolveFile(value).getCanonicalPath());
                                property = property.substring(0, property.indexOf("file.contents") - 1);
                            }
                            c.put(property, value);
                        }
                    }
                }
            }

            _log.info("generator.parse(\"" + controlTemplate + "\", c);");
            generator.parse(controlTemplate, c);

            // - - - - - - - - - - - - - - - - - - - -
            // Remove writing output file of velocity.
            // - - - - - - - - - - - - - - - - - - - -
            // final String parsedString = generator.parse(controlTemplate, c);
            // writer.write(parsedString);
            // writer.flush();
            // writer.close();

            generator.shutdown();
            cleanup();
        } catch (BuildException e) {
            throw e;
        } catch (MethodInvocationException e) {
            throw new IllegalStateException("Exception thrown by '" + e.getReferenceName() + "." + e.getMethodName()
                    + "'" + ". For more information consult the velocity log, or invoke ant with the -debug flag.", e
                    .getWrappedThrowable());
        } catch (ParseErrorException e) {
            throw new IllegalStateException(
                    "Velocity syntax error. For more information consult the velocity log, or invoke ant with the -debug flag.",
                    e);
        } catch (ResourceNotFoundException e) {
            throw new IllegalStateException(
                    "Resource not found. For more information consult the velocity log, or invoke ant with the -debug flag.",
                    e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Generation failed. For more information consult the velocity log, or invoke ant with the -debug flag.",
                    e);
        }
    }

    // Copy from velocity.
    private static String fileContentsToString(String file) {
        String contents = "";
        File f = new File(file);
        if (f.exists())
            try {
                FileReader fr = new FileReader(f);
                char template[] = new char[(int) f.length()];
                fr.read(template);
                contents = new String(template);
            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }
        return contents;
    }

    // -----------------------------------------------------
    //                                           Data Source
    //                                           -----------
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

    // -----------------------------------------------------
    //                                    Context Properties
    //                                    ------------------
    public void setContextProperties(String file) {
        try {
            // /------------------------------------------------------------
            // Initialize internal context properties as ExtendedProperties.
            //   This property is used by Velocity Framework.
            // -------/
            super.setContextProperties(file);
            {
                final Hashtable<?, ?> env = super.getProject().getProperties();
                for (final Iterator<?> ite = env.keySet().iterator(); ite.hasNext();) {
                    final String key = (String) ite.next();
                    if (key.startsWith("torque.")) {
                        String newKey = key.substring("torque.".length());
                        for (int j = newKey.indexOf("."); j != -1; j = newKey.indexOf(".")) {
                            newKey = newKey.substring(0, j) + DfStringUtil.initCap(newKey.substring(j + 1));
                        }
                        contextProperties.setProperty(newKey, (String) env.get(key));
                    }
                }
            }

            // /---------------------------------------------------------------------------------------------------
            // Initialize torque properties as Properties and set up singleton class that saves 'build.properties'.
            //   This property is used by You.
            // -------/
            final Properties prop = DfAntTaskUtil.getBuildProperties(file, super.project);
            DfBuildProperties.getInstance().setProperties(prop);

        } catch (Exception e) {
            _log.warn("setContextProperties() threw the exception!!!", e);
        }
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
    //                                                                    Refresh Resource
    //                                                                    ================
    protected void refreshResources() {
        if (!isRefresh()) {
            return;
        }

        final String projectName = getRefreshProjectName();
        final StringBuilder sb = new StringBuilder().append("refresh?");

        // Refresh the project!
        sb.append(projectName).append("=INFINITE");

        final URL url = getRefreshRequestURL(sb.toString());
        if (url == null) {
            return;
        }

        InputStream is = null;
        try {
            _log.info("/- - - - - - - - - - - - - - - - - - - - - - - -");
            _log.info("...Refreshing the project: " + projectName);
            URLConnection connection = url.openConnection();
            connection.setReadTimeout(getRefreshRequestReadTimeout());
            connection.connect();
            is = connection.getInputStream();
            _log.info("");
            _log.info("    --> OK, Look the refreshed project!");
            _log.info("- - - - - - - - - -/");
            _log.info("");
        } catch (IOException ignored) {
            _log.info("");
            _log.info("    --> Oh, no! " + ignored.getMessage() + ": " + url);
            _log.info("- - - - - - - - - -/");
            _log.info("");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    protected boolean isRefresh() {
        final DfRefreshProperties prop = getProperties().getRefreshProperties();
        return prop.hasRefreshDefinition();
    }

    protected int getRefreshRequestReadTimeout() {
        return 3 * 1000;
    }

    protected String getRefreshProjectName() {
        final DfRefreshProperties prop = getProperties().getRefreshProperties();
        return prop.getProjectName();
    }

    protected URL getRefreshRequestURL(String path) {
        final DfRefreshProperties prop = getProperties().getRefreshProperties();
        String requestUrl = prop.getRequestUrl();
        if (requestUrl.length() > 0) {
            if (!requestUrl.endsWith("/")) {
                requestUrl = requestUrl + "/";
            }
            try {
                return new URL(requestUrl + path);
            } catch (MalformedURLException e) {
                _log.warn("The URL was invalid: " + requestUrl, e);
                return null;
            }
        } else {
            return null;
        }
    }

    // ===================================================================================
    //                                                                    Skip Information
    //                                                                    ================
    protected void showSkippedFileInformation() {
        boolean skipGenerateIfSameFile = getProperties().getLittleAdjustmentProperties().isSkipGenerateIfSameFile();
        if (!skipGenerateIfSameFile) {
            _log.info("/- - - - - - - - - - - - - - - - - - - - - - - -");
            _log.info("All files have been generated. (overrided)");
            _log.info("- - - - - - - - - -/");
            _log.info("");
            return;
        }
        List<String> parseFileNameList = DfGenerator.getInstance().getParseFileNameList();
        List<String> skipFileNameList = DfGenerator.getInstance().getSkipFileNameList();
        _log.info("/- - - - - - - - - - - - - - - - - - - - - - - -");
        _log.info("Several files have skipped generating");
        _log.info("        because they have no changing.");
        _log.info("");
        _log.info("    --> " + skipFileNameList.size() + " / " + parseFileNameList.size());
        _log.info("- - - - - - - - - -/");
        _log.info("");
    }

    // ===================================================================================
    //                                                                          Properties
    //                                                                          ==========
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
    //                                                                       Assist Helper
    //                                                                       =============
    public DfGenerator getGeneratorHandler() {
        return DfGenerator.getInstance();
    }

    // ===================================================================================
    //                                                                      General Helper
    //                                                                      ==============
    protected String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getTargetDatabase() {
        return _targetDatabase;
    }

    public void setTargetDatabase(String v) {
        _targetDatabase = v;
    }

    public void setEnvironmentType(String environmentType) {
        DfEnvironmentType.getInstance().setEnvironmentType(environmentType);
    }
}