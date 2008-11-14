package org.seasar.dbflute.properties;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.seasar.dbflute.DfBuildProperties;
import org.seasar.dbflute.util.basic.DfStringUtil;

/**
 * @author jflute
 * @since 0.7.5 (2008/06/25 Wednesday)
 */
public final class DfOutsideSqlProperties extends DfAbstractHelperProperties {

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public DfOutsideSqlProperties(Properties prop) {
        super(prop);
    }

    // ===================================================================================
    //                                                             outsideSqlDefinitionMap
    //                                                             =======================
    public static final String KEY_outsideSqlDefinitionMap = "outsideSqlDefinitionMap";
    protected Map<String, Object> _outsideSqlDefinitionMap;

    protected Map<String, Object> getOutsideSqlDefinitionMap() {
        if (_outsideSqlDefinitionMap == null) {
            _outsideSqlDefinitionMap = mapProp("torque." + KEY_outsideSqlDefinitionMap, DEFAULT_EMPTY_MAP);
        }
        return _outsideSqlDefinitionMap;
    }

    // ===================================================================================
    //                                                                     SqlFileEncoding
    //                                                                     ===============
    public boolean hasSqlFileEncoding() {
        final String encoding = getSqlFileEncoding();
        return encoding != null && encoding.trim().length() > 0 && !encoding.trim().equalsIgnoreCase("null");
    }

    public String getSqlFileEncoding() {
        final String value = (String) getOutsideSqlDefinitionMap().get("sqlFileEncoding");
        if (value != null && value.trim().length() > 0 && !value.trim().equalsIgnoreCase("null")) {
            return value;
        }
        DfS2DaoAdjustmentProperties prop = DfBuildProperties.getInstance().getS2DaoAdjustmentProperties();
        if (prop.hasDaoSqlFileEncoding()) {
            return prop.getDaoSqlFileEncoding();
        }
        return "UTF-8";
    }

    // ===================================================================================
    //                                                                        SqlDirectory
    //                                                                        ============
    public String getSqlDirectory() {
        String sqlDirectory = (String) getOutsideSqlDefinitionMap().get("sqlDirectory");
        if (sqlDirectory == null || sqlDirectory.trim().length() == 0) {
            sqlDirectory = getDefaultSqlDirectory();
        }
        sqlDirectory = removeEndSeparatorIfNeeds(sqlDirectory);
        String sqlPackage = getSqlPackage();
        if (sqlPackage != null && sqlPackage.trim().length() > 0) {
            String sqlPackageDirectory = resolveFileSeparator(sqlPackage);
            sqlDirectory = sqlDirectory + "/" + removeStartSeparatorIfNeeds(sqlPackageDirectory);
        }
        return sqlDirectory;
    }

    protected String getDefaultSqlDirectory() {
        return getBasicProperties().getOutputDirectory();
    }

    protected String resolveFileSeparator(String sqlPackage) {
        DfLittleAdjustmentProperties prop = getLittleAdjustmentProperties();
        if (!prop.isFlatDirectoryPackageValid()) {
            return replaceDotToSeparator(sqlPackage);
        }
        final String flatDirectoryPackage = prop.getFlatDirectoryPackage();
        if (!sqlPackage.contains(flatDirectoryPackage)) {
            return replaceDotToSeparator(sqlPackage);
        }
        sqlPackage = DfStringUtil.replace(getSqlPackage(), ".", "$$flatDirectoryDelimiter$$");
        sqlPackage = replaceDotToSeparator(sqlPackage);
        sqlPackage = DfStringUtil.replace(getSqlPackage(), "$$flatDirectoryDelimiter$$", ".");
        return sqlPackage;
    }

    protected String replaceDotToSeparator(String sqlPackage) {
        return DfStringUtil.replace(getSqlPackage(), ".", "/");
    }

    protected DfLittleAdjustmentProperties getLittleAdjustmentProperties() {
        return DfBuildProperties.getInstance().getLittleAdjustmentProperties();
    }

    protected String removeStartSeparatorIfNeeds(String path) {
        if (path.startsWith("/")) {
            return path.substring("/".length());
        }
        return path;
    }

    protected String removeEndSeparatorIfNeeds(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    // ===================================================================================
    //                                                                          SqlPackage
    //                                                                          ==========
    public boolean isSqlPackageValid() {
        final String sqlPackage = getSqlPackage();
        return sqlPackage != null && sqlPackage.trim().length() > 0 && !sqlPackage.trim().equalsIgnoreCase("null");
    }

    public String getSqlPackage() {
        String sqlPackage = (String) getOutsideSqlDefinitionMap().get("sqlPackage");
        if (sqlPackage == null || sqlPackage.trim().length() == 0) {
            sqlPackage = getDefaultSqlPackage();
        }
        return resolvePackageBaseMarkIfNeeds(sqlPackage);
    }

    protected String getDefaultSqlPackage() {
        return "";
    }

    protected String resolvePackageBaseMarkIfNeeds(String sqlPackage) {
        DfGeneratedClassPackageProperties packageProperties = getBasicProperties().getGeneratedClassPackageProperties();
        String packageBase = packageProperties.getPackageBase();
        return DfStringUtil.replace(sqlPackage, "$$PACKAGE_BASE$$", packageBase);
    }

    // ===================================================================================
    //                                                                      DefaultPackage
    //                                                                      ==============
    public boolean isDefaultPackageValid() { // C# only
        return getDefaultPackage() != null && getDefaultPackage().trim().length() > 0
                && !getDefaultPackage().trim().equalsIgnoreCase("null");
    }

    public String getDefaultPackage() { // C# only
        return (String) getOutsideSqlDefinitionMap().get("defaultPackage");
    }

    // ===================================================================================
    //                                                             OmitResourcePathPackage
    //                                                             =======================
    public boolean isOmitResourcePathPackageValid() { // C# only
        return getOmitResourcePathPackage() != null && getOmitResourcePathPackage().trim().length() > 0
                && !getOmitResourcePathPackage().trim().equalsIgnoreCase("null");
    }

    public String getOmitResourcePathPackage() { // C# only
        return (String) getOutsideSqlDefinitionMap().get("omitResourcePathPackage");
    }

    // ===================================================================================
    //                                                           OmitFileSystemPathPackage
    //                                                           =========================
    public boolean isOmitFileSystemPathPackageValid() { // C# only
        return getOmitFileSystemPathPackage() != null && getOmitFileSystemPathPackage().trim().length() > 0
                && !getOmitFileSystemPathPackage().trim().equalsIgnoreCase("null");
    }

    public String getOmitFileSystemPathPackage() { // C# only
        return (String) getOutsideSqlDefinitionMap().get("omitFileSystemPathPackage");
    }

    // ===================================================================================
    //                                                                     OutputDirectory
    //                                                                     ===============
    public String getSql2EntityOutputDirectory() {
        final String value = (String) getOutsideSqlDefinitionMap().get("sql2EntityOutputDirectory");
        if (value == null || value.trim().length() == 0) {
            return getBasicProperties().getOutputDirectory();
        } else {
            return value;
        }
    }

    // ===================================================================================
    //                                                             Procedure ParameterBean
    //                                                             =======================
    public boolean isGenerateProcedureParameterBean() {
        String value = (String) getOutsideSqlDefinitionMap().get("generateProcedureParameterBean");
        return value != null && value.trim().equalsIgnoreCase("true");
    }

    public boolean isTargetProcedureCatalog(String procedureCatalog) {
        final List<String> targetProcedureList = getTargetProcedureCatalogList();
        if (targetProcedureList == null || targetProcedureList.isEmpty()) {
            return true;
        }
        if (procedureCatalog == null || procedureCatalog.trim().length() == 0) {
            if (targetProcedureList.contains("$$DEFAULT$$")) {
                return true;
            } else {
                return false;
            }
        }
        for (String catalogHint : targetProcedureList) {
            if (isHitByTheHint(procedureCatalog, catalogHint)) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getTargetProcedureCatalogList() {
        return getOutsideSqlPropertyAsList("targetProcedureCatalogList");
    }

    public boolean isTargetProcedureSchema(String procedureSchema) {
        final List<String> targetProcedureList = getTargetProcedureSchemaList();
        if (targetProcedureList == null || targetProcedureList.isEmpty()) {
            return true;
        }
        if (procedureSchema == null || procedureSchema.trim().length() == 0) {
            if (targetProcedureList.contains("$$DEFAULT$$")) {
                return true;
            } else {
                return false;
            }
        }
        for (String schemaHint : targetProcedureList) {
            if (isHitByTheHint(procedureSchema, schemaHint)) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getTargetProcedureSchemaList() {
        return getOutsideSqlPropertyAsList("targetProcedureSchemaList");
    }

    public boolean isTargetProcedureName(String procedureName) {
        final List<String> targetProcedureList = getTargetProcedureNameList();
        if (targetProcedureList == null || targetProcedureList.isEmpty()) {
            return true;
        }
        for (String procedureNameHint : targetProcedureList) {
            if (isHitByTheHint(procedureName, procedureNameHint)) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getTargetProcedureNameList() {
        return getOutsideSqlPropertyAsList("targetProcedureNameList");
    }

    // ===================================================================================
    //                                                                      Package Detail
    //                                                                      ==============
    protected String getSpecifiedBaseCustomizeEntityPackage() {
        final String value = (String) getOutsideSqlDefinitionMap().get("baseCustomizeEntityPackage");
        return (value != null && value.trim().length() > 0) ? value : null;
    }

    protected String getSpecifiedExtendedCustomizeEntityPackage() {
        final String value = (String) getOutsideSqlDefinitionMap().get("extendedCustomizeEntityPackage");
        return (value != null && value.trim().length() > 0) ? value : null;
    }

    protected String getSpecifiedBaseParameterBeanPackage() {
        final String value = (String) getOutsideSqlDefinitionMap().get("baseParameterBeanPackage");
        return (value != null && value.trim().length() > 0) ? value : null;
    }

    protected String getSpecifiedExtendedParameterBeanPackage() {
        final String value = (String) getOutsideSqlDefinitionMap().get("extendedParameterBeanPackage");
        return (value != null && value.trim().length() > 0) ? value : null;
    }

    public String getBaseEntityPackage() {
        String specifiedPackage = getSpecifiedBaseCustomizeEntityPackage();
        if (specifiedPackage != null && specifiedPackage.trim().length() != 0) {
            return specifiedPackage;
        }
        DfGeneratedClassPackageProperties packageProperties = getGeneratedClassPackageProperties();
        String defaultPackage = packageProperties.getBaseEntityPackage() + "." + getCustomizePackageName();
        if (defaultPackage != null && defaultPackage.trim().length() != 0) {
            return defaultPackage;
        } else {
            String msg = "Both packageString in sql2entity-property and baseEntityPackage are null.";
            throw new IllegalStateException(msg);
        }
    }

    public String getDBMetaPackage() {
        String dbmetaPackage = "dbmeta";
        if (getBasicProperties().isTargetLanguageCSharp()) {
            dbmetaPackage = "Dbm";
        }
        return getBaseEntityPackage() + "." + dbmetaPackage;
    }

    public String getExtendedEntityPackage() {
        String specifiedPackage = getSpecifiedExtendedCustomizeEntityPackage();
        if (specifiedPackage != null && specifiedPackage.trim().length() != 0) {
            return specifiedPackage;
        }
        DfGeneratedClassPackageProperties packageProperties = getGeneratedClassPackageProperties();
        String defaultPackage = packageProperties.getExtendedEntityPackage() + "." + getCustomizePackageName();
        if (defaultPackage != null && defaultPackage.trim().length() != 0) {
            return defaultPackage;
        } else {
            String msg = "Both packageString in sql2entity-property and extendedEntityPackage are null.";
            throw new IllegalStateException(msg);
        }
    }

    protected String getCustomizePackageName() {
        String customizePackage = "customize";
        if (getBasicProperties().isTargetLanguageCSharp()) {
            customizePackage = "Customize";
        }
        return customizePackage;
    }

    public String getBaseCursorPackage() {
        DfGeneratedClassPackageProperties packageProperties = getGeneratedClassPackageProperties();
        if (isMakeDaoInterface()) {
            return packageProperties.getBaseDaoPackage() + "." + getCursorPackageName();
        } else {
            return packageProperties.getBaseBehaviorPackage() + "." + getCursorPackageName();
        }
    }

    public String getExtendedCursorPackage() {
        DfGeneratedClassPackageProperties packageProperties = getGeneratedClassPackageProperties();
        if (isMakeDaoInterface()) {
            return packageProperties.getExtendedDaoPackage() + "." + getCursorPackageName();
        } else {
            return packageProperties.getExtendedBehaviorPackage() + "." + getCursorPackageName();
        }
    }

    protected String getCursorPackageName() {
        String pmbeanPackage = "cursor";
        if (getBasicProperties().isTargetLanguageCSharp()) {
            pmbeanPackage = "Cursor";
        }
        return pmbeanPackage;
    }

    public String getBaseParameterBeanPackage() {
        String specifiedPackage = getSpecifiedBaseParameterBeanPackage();
        if (specifiedPackage != null && specifiedPackage.trim().length() != 0) {
            return specifiedPackage;
        }

        DfGeneratedClassPackageProperties packageProperties = getGeneratedClassPackageProperties();
        final String defaultPackage;
        if (isMakeDaoInterface()) {
            defaultPackage = packageProperties.getBaseDaoPackage() + "." + getPmbeanPackageName();
        } else {
            defaultPackage = packageProperties.getBaseBehaviorPackage() + "." + getPmbeanPackageName();
        }
        if (defaultPackage != null && defaultPackage.trim().length() != 0) {
            return defaultPackage;
        } else {
            String msg = "Both packageString in sql2entity-property and baseEntityPackage are null.";
            throw new IllegalStateException(msg);
        }
    }

    public String getExtendedParameterBeanPackage() {
        String specifiedPackage = getSpecifiedExtendedParameterBeanPackage();
        if (specifiedPackage != null && specifiedPackage.trim().length() != 0) {
            return specifiedPackage;
        }
        DfGeneratedClassPackageProperties packageProperties = getGeneratedClassPackageProperties();
        final String defaultPackage;
        if (isMakeDaoInterface()) {
            defaultPackage = packageProperties.getExtendedDaoPackage() + "." + getPmbeanPackageName();
        } else {
            defaultPackage = packageProperties.getExtendedBehaviorPackage() + "." + getPmbeanPackageName();
        }
        if (defaultPackage != null && defaultPackage.trim().length() != 0) {
            return defaultPackage;
        } else {
            String msg = "Both packageString in sql2entity-property and extendedEntityPackage are null.";
            throw new IllegalStateException(msg);
        }
    }

    protected String getPmbeanPackageName() {
        String pmbeanPackage = "pmbean";
        if (getBasicProperties().isTargetLanguageCSharp()) {
            pmbeanPackage = "PmBean";
        }
        return pmbeanPackage;
    }

    protected boolean isMakeDaoInterface() {
        final DfMakingOptionProperties prop = DfBuildProperties.getInstance().getMakingOptionProperties();
        return prop.isMakeDaoInterface();
    }

    // ===================================================================================
    //                                                                     Property Helper
    //                                                                     ===============
    @SuppressWarnings("unchecked")
    protected List<String> getOutsideSqlPropertyAsList(String key) {
        return (List<String>) getOutsideSqlDefinitionMap().get(key);
    }
}