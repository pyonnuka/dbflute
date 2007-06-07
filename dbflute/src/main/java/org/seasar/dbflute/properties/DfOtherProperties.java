package org.seasar.dbflute.properties;

import java.util.Map;
import java.util.Properties;

import org.seasar.dbflute.helper.flexiblename.DfFlexibleNameMap;
import org.seasar.framework.util.StringUtil;

/**
 * Build properties for Torque.
 * 
 * @author jflute
 */
public final class DfOtherProperties extends DfAbstractHelperProperties {

    //    private static final Log _log = LogFactory.getLog(GeneratedClassPackageProperties.class);

    /**
     * Constructor.
     */
    public DfOtherProperties(Properties prop) {
        super(prop);
    }

    // ===============================================================================
    //                                                              Properties - Other
    //                                                              ==================
    public boolean isStopGenerateExtendedBhv() {
        return booleanProp("torque.isStopGenerateExtendedBhv", false);
    }

    public boolean isStopGenerateExtendedDao() {
        return booleanProp("torque.isStopGenerateExtendedDao", false);
    }

    public boolean isStopGenerateExtendedEntity() {
        return booleanProp("torque.isStopGenerateExtendedEntity", false);
    }

    // ===============================================================================
    //                                                                   S2Dao Version
    //                                                                   =============
    public boolean isVersionAfter1043() {
        if (!hasS2DaoVersion()) {
            return true;
        }
        return isS2DaoVersionGreaterEqual("1.0.43");
    }

    public boolean isVersionAfter1040() {
        if (!hasS2DaoVersion()) {
            return booleanProp("torque.isVersionAfter1040", true);
        }
        return isS2DaoVersionGreaterEqual("1.0.40");
    }

    protected boolean hasS2DaoVersion() {
        return stringProp("torque.s2daoVersion", null) != null;
    }

    protected String getS2DaoVersion() {
        final String s2daoVersion = stringProp("torque.s2daoVersion", null);
        return s2daoVersion != null ? StringUtil.replace(s2daoVersion, ".", "") : "9.9.99";// If null, return the latest version!
    }

    protected boolean isS2DaoVersionGreaterEqual(String targetVersion) {
        final String s2daoVersion = getS2DaoVersion();
        final String filteredTargetVersion = StringUtil.replace(targetVersion, ".", "");
        return s2daoVersion.compareToIgnoreCase(filteredTargetVersion) >= 0;
    }

    // ===============================================================================
    //                                                                  S2Dao Override
    //                                                                  ==============
    public boolean isAvailableOtherConnectionDaoInitialization() {
        return booleanProp("torque.isAvailableOtherConnectionDaoInitialization", false);
    }

    public boolean isAvailableDaoMethodLazyInitializing() {
        return booleanProp("torque.isAvailableDaoMethodLazyInitializing", false);
    }
    
    // ===============================================================================
    //                                                         Non PrimaryKey Writable
    //                                                         =======================
    public boolean isAvailableNonPrimaryKeyWritable() {
        return booleanProp("torque.isAvailableNonPrimaryKeyWritable", false);
    }

    // ===============================================================================
    //                                                             MultipleFK Property
    //                                                             ===================
    public static final String KEY_multipleFKPropertyMap = "multipleFKPropertyMap";
    protected Map<String, Map<String, Map<String, String>>> _multipleFKPropertyMap;

    public Map<String, Map<String, Map<String, String>>> getMultipleFKPropertyMap() {
        if (_multipleFKPropertyMap == null) {
            // TODO: @jflute - 真面目に展開すること。
            final Object obj = mapProp("torque." + KEY_multipleFKPropertyMap, DEFAULT_EMPTY_MAP);
            _multipleFKPropertyMap = (Map<String, Map<String, Map<String, String>>>) obj;
        }

        return _multipleFKPropertyMap;
    }

    public DfFlexibleNameMap<String, Map<String, Map<String, String>>> getMultipleFKPropertyMapAsFlexible() {
        return new DfFlexibleNameMap<String, Map<String, Map<String, String>>>(getMultipleFKPropertyMap());
    }

    public String getMultipleFKPropertyColumnAliasName(String tableName, java.util.List<String> columnNameList) {
        final Map<String, Map<String, String>> foreignKeyMap = getMultipleFKPropertyMapAsFlexible().get(tableName);
        if (foreignKeyMap == null) {
            return "";
        }
        final String columnKey = createMultipleFKPropertyColumnKey(columnNameList);
        final DfFlexibleNameMap<String, Map<String, String>> foreignKeyFxMap = getMultipleFKPropertyForeignKeyMapAsFlexible(foreignKeyMap);
        final Map<String, String> foreignPropertyElement = foreignKeyFxMap.get(columnKey);
        if (foreignPropertyElement == null) {
            return "";
        }
        final String columnAliasName = foreignPropertyElement.get("columnAliasName");
        return columnAliasName;
    }

    protected String createMultipleFKPropertyColumnKey(java.util.List<String> columnNameList) {
        final StringBuilder sb = new StringBuilder();
        for (String columnName : columnNameList) {
            sb.append("/").append(columnName);
        }
        sb.delete(0, "/".length());
        return sb.toString();
    }

    protected DfFlexibleNameMap<String, Map<String, String>> getMultipleFKPropertyForeignKeyMapAsFlexible(
            final Map<String, Map<String, String>> foreignKeyMap) {
        final DfFlexibleNameMap<String, Map<String, String>> foreignKeyFxMap = new DfFlexibleNameMap<String, Map<String, String>>(
                foreignKeyMap);
        return foreignKeyFxMap;
    }
}