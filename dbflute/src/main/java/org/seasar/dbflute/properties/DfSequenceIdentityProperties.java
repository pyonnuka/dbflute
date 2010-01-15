package org.seasar.dbflute.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.seasar.dbflute.exception.DfIllegalPropertyTypeException;
import org.seasar.dbflute.helper.collection.DfFlexibleMap;
import org.seasar.dbflute.logic.factory.DfSequenceHandlerFactory;
import org.seasar.dbflute.logic.jdbc.metadata.info.DfSequenceMetaInfo;
import org.seasar.dbflute.logic.jdbc.metadata.sequence.DfSequenceHandler;

/**
 * @author jflute
 */
public final class DfSequenceIdentityProperties extends DfAbstractHelperProperties {

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public DfSequenceIdentityProperties(Properties prop) {
        super(prop);
    }

    // ===================================================================================
    //                                                             Sequence Definition Map
    //                                                             =======================
    protected static final String KEY_sequenceDefinitionMap = "sequenceDefinitionMap";
    protected Map<String, String> _sequenceDefinitionMap;

    protected Map<String, String> getSequenceDefinitionMap() {
        if (_sequenceDefinitionMap == null) {
            LinkedHashMap<String, String> tmpMap = new LinkedHashMap<String, String>();
            Map<String, Object> originalMap = mapProp("torque." + KEY_sequenceDefinitionMap, DEFAULT_EMPTY_MAP);
            Set<Entry<String, Object>> entrySet = originalMap.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                String tableName = entry.getKey();
                Object sequenceValue = entry.getValue();
                if (!(sequenceValue instanceof String)) {
                    String msg = "The value of sequence map should be string:";
                    msg = msg + " sequenceValue=" + sequenceValue + " map=" + originalMap;
                    throw new DfIllegalPropertyTypeException(msg);
                }
                tmpMap.put(tableName, (String) sequenceValue);
            }
            _sequenceDefinitionMap = tmpMap;
        }
        return _sequenceDefinitionMap;
    }

    public Map<String, String> getTableSequenceMap() {
        final Map<String, String> sequenceDefinitionMap = getSequenceDefinitionMap();
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        final Set<String> keySet = sequenceDefinitionMap.keySet();
        for (String tableName : keySet) {
            resultMap.put(tableName, getSequenceName(tableName));
        }
        return resultMap;
    }

    public String getSequenceName(String flexibleTableName) {
        final DfFlexibleMap<String, String> flmap = new DfFlexibleMap<String, String>(getSequenceDefinitionMap());
        final String sequence = flmap.get(flexibleTableName);
        if (sequence == null) {
            return null;
        }
        final String hintMark = ":";
        final int hintMarkIndex = sequence.lastIndexOf(hintMark);
        if (hintMarkIndex < 0) {
            return sequence;
        }
        return sequence.substring(0, hintMarkIndex);
    }

    public String getSequenceCacheSize(DataSource dataSource, String schemaName, String flexibleTableName) {
        final DfFlexibleMap<String, String> flmap = new DfFlexibleMap<String, String>(getSequenceDefinitionMap());
        final String sequence = flmap.get(flexibleTableName);
        if (sequence == null) {
            return null;
        }
        final String hintMark = ":";
        final int hintMarkIndex = sequence.lastIndexOf(hintMark);
        if (hintMarkIndex < 0) {
            return null;
        }
        final String hint = sequence.substring(hintMarkIndex + hintMark.length()).trim();
        final String incrementMark = "cache(";
        final int incrementMarkIndex = hint.indexOf(incrementMark);
        if (incrementMarkIndex < 0) {
            return null;
        }
        final String cacheValue = hint.substring(incrementMarkIndex + incrementMark.length()).trim();
        final String endMark = ")";
        final int endMarkIndex = cacheValue.indexOf(endMark);
        if (endMarkIndex < 0) {
            String msg = "The increment size setting needs end mark ')':";
            msg = msg + " sequence=" + sequence;
            throw new IllegalStateException(msg);
        }
        final String cacheSize = cacheValue.substring(0, endMarkIndex).trim();
        if (cacheSize != null && cacheSize.trim().length() > 0) {
            return cacheSize;
        }
        final String incrementSize = getSequenceIncrementSize(dataSource, schemaName, flexibleTableName);
        if (incrementSize != null) {
            return incrementSize;
        }
        String msg = "Failed to get the cache size of sequence:";
        msg = msg + " schema=" + schemaName + " table=" + flexibleTableName + " sequence=" + sequence;
        throw new IllegalStateException(msg);
    }

    protected String getSequenceIncrementSize(DataSource dataSource, String schemaName, String flexibleTableName) {
        final String sequenceName = getSequenceName(flexibleTableName);
        if (sequenceName == null) {
            return null;
        }
        final Map<String, DfSequenceMetaInfo> sequenceMetaInfoMap = getSequenceMetaInfoMap(dataSource);
        final String sequenceInfoKey = (schemaName != null ? schemaName + "." : "") + sequenceName;
        final DfSequenceMetaInfo info = sequenceMetaInfoMap.get(sequenceInfoKey);
        if (info != null) {
            final Integer incrementSize = info.getIncrementSize();
            if (incrementSize != null) {
                return incrementSize.toString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected Map<String, DfSequenceMetaInfo> _sequenceMetaInfoMap;

    protected Map<String, DfSequenceMetaInfo> getSequenceMetaInfoMap(DataSource dataSource) {
        if (_sequenceMetaInfoMap != null) {
            return _sequenceMetaInfoMap;
        }
        final DfSequenceHandlerFactory factory = new DfSequenceHandlerFactory(dataSource, getBasicProperties(),
                getDatabaseProperties());
        final DfSequenceHandler sequenceHandler = factory.createSequenceHandler();
        if (sequenceHandler != null) {
            _sequenceMetaInfoMap = sequenceHandler.getSequenceMap();
        } else {
            _sequenceMetaInfoMap = new HashMap<String, DfSequenceMetaInfo>();
        }
        return _sequenceMetaInfoMap;
    }

    /**
     * @param checker The checker for call-back. (NotNull)
     */
    public void checkSequenceDefinitionMap(SequenceDefinitionMapChecker checker) {
        final Map<String, String> sequenceDefinitionMap = getSequenceDefinitionMap();
        final Set<String> keySet = sequenceDefinitionMap.keySet();
        final List<String> notFoundTableNameList = new ArrayList<String>();
        for (String tableName : keySet) {
            if (!checker.hasTable(tableName)) {
                notFoundTableNameList.add(tableName);
            }
        }
        if (!notFoundTableNameList.isEmpty()) {
            throwSequenceDefinitionMapNotFoundTableException(notFoundTableNameList);
        }
    }

    protected void throwSequenceDefinitionMapNotFoundTableException(List<String> notFoundTableNameList) {
        String msg = "Look! Read the message below." + getLineSeparator();
        msg = msg + "/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *" + getLineSeparator();
        msg = msg + "The table name was Not Found in the map of sequence definition!" + getLineSeparator();
        msg = msg + getLineSeparator();
        msg = msg + "[Not Found Table]" + getLineSeparator();
        for (String tableName : notFoundTableNameList) {
            msg = msg + tableName + getLineSeparator();
        }
        msg = msg + getLineSeparator();
        msg = msg + "[Sequence Definition]" + getLineSeparator() + _sequenceDefinitionMap + getLineSeparator();
        msg = msg + "* * * * * * * * * */";
        throw new SequenceDefinitionMapTableNotFoundException(msg);
    }

    public static interface SequenceDefinitionMapChecker {
        public boolean hasTable(String tableName);
    }

    public static class SequenceDefinitionMapTableNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public SequenceDefinitionMapTableNotFoundException(String msg) {
            super(msg);
        }
    }

    protected String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    // ===================================================================================
    //                                                                Sequence Return Type
    //                                                                ====================
    public String getSequenceReturnType() { // It's not property!
        return getBasicProperties().getLanguageDependencyInfo().getDefaultSequenceType();
    }

    // ===================================================================================
    //                                                             Identity Definition Map
    //                                                             =======================
    protected static final String KEY_identityDefinitionMap = "identityDefinitionMap";
    protected Map<String, Object> _identityDefinitionMap;

    // # /---------------------------------------------------------------------------
    // # identityDefinitionMap: (Default 'map:{}')
    // # 
    // # The relation mappings between identity and column of table.
    // # Basically you don't need this property because DBFlute
    // # can get the information about identity from JDBC automatically.
    // # The table names and column names are treated as case insensitive.
    // # 
    // # Example:
    // # map:{
    // #     ; PURCHASE     = PURCHASE_ID
    // #     ; MEMBER       = MEMBER_ID
    // #     ; MEMBER_LOGIN = MEMBER_LOGIN_ID
    // #     ; PRODUCT      = PRODUCT_ID
    // # }
    // #
    // # *The line that starts with '#' means comment-out.
    // #
    // map:{
    //     #; PURCHASE     = PURCHASE_ID
    //     #; MEMBER       = MEMBER_ID
    //     #; MEMBER_LOGIN = MEMBER_LOGIN_ID
    //     #; PRODUCT      = PRODUCT_ID
    // }
    // # ----------------/

    protected Map<String, Object> getIdentityDefinitionMap() {
        if (_identityDefinitionMap == null) {
            _identityDefinitionMap = mapProp("torque." + KEY_identityDefinitionMap, DEFAULT_EMPTY_MAP);
        }
        return _identityDefinitionMap;
    }

    public String getIdentityColumnName(String flexibleTableName) {
        final DfFlexibleMap<String, Object> flmap = new DfFlexibleMap<String, Object>(getIdentityDefinitionMap());
        return (String) flmap.get(flexibleTableName);
    }
}