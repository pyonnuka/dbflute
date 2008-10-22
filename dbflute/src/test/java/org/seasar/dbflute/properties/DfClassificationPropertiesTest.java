package org.seasar.dbflute.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author jflute
 * @since 0.6.2 (2008/01/26 Saturday)
 */
public class DfClassificationPropertiesTest {

    protected static final String ALL_MARK = DfClassificationProperties.MARK_classificationDeploymentAllTable;

    @Test
    public void test_hasClassification_SameCase() throws Exception {
        // ## Arrange ##
        final Map<String, Map<String, String>> deploymentMap = new LinkedHashMap<String, Map<String, String>>();
        {
            final Map<String, String> columnClassificationMap = new LinkedHashMap<String, String>();
            columnClassificationMap.put("MEMBER_STATUS_CODE", "MemberStatus");
            deploymentMap.put("MEMBER_STATUS", columnClassificationMap);
        }
        final DfClassificationProperties prop = createClassificationProperties(deploymentMap);

        // ## Act ##
        final boolean actual = prop.hasClassification("MEMBER_STATUS", "MEMBER_STATUS_CODE");

        // ## Assert ##
        Assert.assertTrue(actual);
    }

    @Test
    public void test_hasClassification_IgnoreCase() throws Exception {
        // ## Arrange ##
        final Map<String, Map<String, String>> deploymentMap = new LinkedHashMap<String, Map<String, String>>();
        {
            final Map<String, String> columnClassificationMap = new LinkedHashMap<String, String>();
            columnClassificationMap.put("MEMBER_STATUS_CODE", "MemberStatus");
            deploymentMap.put("MEMBER_STATUS", columnClassificationMap);
        }
        final DfClassificationProperties prop = createClassificationProperties(deploymentMap);

        // ## Act ##
        final boolean actual = prop.hasClassification("member_status", "member_status_code");

        // ## Assert ##
        Assert.assertTrue(actual);
    }

    @Test
    public void test_isAllClassificationColumn_ByPinpoint() {
        // ## Arrange ##
        final Map<String, Map<String, String>> deploymentMap = new LinkedHashMap<String, Map<String, String>>();
        {
            final Map<String, String> columnClassificationMap = new LinkedHashMap<String, String>();
            columnClassificationMap.put("ABC", "abc");
            columnClassificationMap.put("DELETE_FLG", "Flg");
            columnClassificationMap.put("DEF", "def");
            deploymentMap.put(ALL_MARK, columnClassificationMap);
        }
        final DfClassificationProperties prop = createClassificationProperties(deploymentMap);

        // ## Act & Assert ##
        assertTrue(prop.isAllClassificationColumn("DELETE_FLG"));
        assertTrue(prop.isAllClassificationColumn("delete_flg"));
        assertFalse(prop.isAllClassificationColumn("VALID_FLG"));
        assertFalse(prop.isAllClassificationColumn("valid_flg"));
    }

    @Test
    public void test_isAllClassificationColumn_ByHint() {
        // ## Arrange ##
        final Map<String, Map<String, String>> deploymentMap = new LinkedHashMap<String, Map<String, String>>();
        {
            final Map<String, String> columnClassificationMap = new LinkedHashMap<String, String>();
            columnClassificationMap.put("ABC", "abc");
            columnClassificationMap.put("suffix:_FLG", "Flg");
            columnClassificationMap.put("DEF", "def");
            deploymentMap.put(ALL_MARK, columnClassificationMap);
        }
        final DfClassificationProperties prop = createClassificationProperties(deploymentMap);

        // ## Act & Assert ##
        assertTrue(prop.isAllClassificationColumn("DELETE_FLG"));
        assertTrue(prop.isAllClassificationColumn("delete_flg"));
        assertTrue(prop.isAllClassificationColumn("VALID_FLG"));
        assertTrue(prop.isAllClassificationColumn("valid_flg"));
    }

    @Test
    public void test_getAllClassificationName_ByPinpoint() {
        // ## Arrange ##
        final Map<String, Map<String, String>> deploymentMap = new LinkedHashMap<String, Map<String, String>>();
        {
            final Map<String, String> columnClassificationMap = new LinkedHashMap<String, String>();
            columnClassificationMap.put("ABC", "abc");
            columnClassificationMap.put("DELETE_FLG", "Flg");
            columnClassificationMap.put("DEF", "def");
            deploymentMap.put(ALL_MARK, columnClassificationMap);
        }
        final DfClassificationProperties prop = createClassificationProperties(deploymentMap);

        // ## Act & Assert ##
        assertEquals("Flg", prop.getAllClassificationName("DELETE_FLG"));
        assertEquals("Flg", prop.getAllClassificationName("delete_flg"));
        assertNull(prop.getAllClassificationName("VALID_FLG"));
        assertNull(prop.getAllClassificationName("valid_flg"));
    }

    @Test
    public void test_getAllClassificationName_ByHint() {
        // ## Arrange ##
        final Map<String, Map<String, String>> deploymentMap = new LinkedHashMap<String, Map<String, String>>();
        {
            final Map<String, String> columnClassificationMap = new LinkedHashMap<String, String>();
            columnClassificationMap.put("ABC", "abc");
            columnClassificationMap.put("suffix:_FLG", "Flg");
            columnClassificationMap.put("DEF", "def");
            deploymentMap.put(ALL_MARK, columnClassificationMap);
        }
        final DfClassificationProperties prop = createClassificationProperties(deploymentMap);

        // ## Act & Assert ##
        assertEquals("Flg", prop.getAllClassificationName("DELETE_FLG"));
        assertEquals("Flg", prop.getAllClassificationName("delete_flg"));
        assertEquals("Flg", prop.getAllClassificationName("VALID_FLG"));
        assertEquals("Flg", prop.getAllClassificationName("valid_flg"));
    }

    protected DfClassificationProperties createClassificationProperties(Map<String, Map<String, String>> deploymentMap) {
        final DfClassificationProperties prop = new DfClassificationProperties(new Properties());
        prop._classificationDeploymentMap = deploymentMap;
        return prop;
    }
}
