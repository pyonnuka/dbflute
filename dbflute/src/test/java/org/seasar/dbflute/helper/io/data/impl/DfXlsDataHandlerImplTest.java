package org.seasar.dbflute.helper.io.data.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.regex.PatternSyntaxException;

import org.junit.Test;
import org.seasar.dbflute.helper.collection.DfFlexibleMap;
import org.seasar.dbflute.helper.jdbc.metadata.info.DfColumnMetaInfo;
import org.seasar.dbflute.unit.DfDBFluteTestCase;

public class DfXlsDataHandlerImplTest extends DfDBFluteTestCase {

    // ===================================================================================
    //                                                                               Write
    //                                                                               =====
    @Test
    public void test_removeDoubleQuotation() {
        // ## Arrange ##
        final DfXlsDataHandlerImpl impl = new DfXlsDataHandlerImpl();

        // ## Act & Assert ##
        assertEquals("aaa", impl.removeDoubleQuotation("\"aaa\""));
        assertEquals("a", impl.removeDoubleQuotation("\"a\""));
        assertEquals("", impl.removeDoubleQuotation("\"\""));
    }

    // ===================================================================================
    //                                                                    Process per Type
    //                                                                    ================
    // -----------------------------------------------------
    //                                     NotNull NotString
    //                                     -----------------
    @Test
    public void test_DfXlsDataHandlerImpl_isNotNullNotString() {
        // ## Arrange ##
        final DfXlsDataHandlerImpl impl = new DfXlsDataHandlerImpl();

        // ## Act & Assert ##
        assertFalse(impl.isNotNullNotString(null));
        assertFalse(impl.isNotNullNotString("abc"));
        assertTrue(impl.isNotNullNotString(new Date()));
        assertTrue(impl.isNotNullNotString(new Timestamp(System.currentTimeMillis())));
    }

    // -----------------------------------------------------
    //                                             Timestamp
    //                                             ---------
    @Test
    public void test_DfXlsDataHandlerImpl_filterTimestampValue() {
        final DfXlsDataHandlerImpl dfXlsDataHandlerImpl = new DfXlsDataHandlerImpl();
        final String filteredTimestampValue = dfXlsDataHandlerImpl.filterTimestampValue("2007/01/01");
        assertEquals("2007-01-01 00:00:00", filteredTimestampValue);
    }

    // -----------------------------------------------------
    //                                               Boolean
    //                                               -------
    @Test
    public void test_processBoolean() throws Exception {
        // ## Arrange ##
        final DfXlsDataHandlerImpl impl = new DfXlsDataHandlerImpl() {
            @Override
            protected Class<?> getColumnType(DfColumnMetaInfo columnMetaInfo) {
                return BigDecimal.class;
            }
        };
        DfFlexibleMap<String, DfColumnMetaInfo> columnMetaInfoMap = new DfFlexibleMap<String, DfColumnMetaInfo>();
        DfColumnMetaInfo info = new DfColumnMetaInfo();
        info.setColumnName("foo");
        info.setColumnSize(3);
        info.setJdbcType(Types.NUMERIC);
        columnMetaInfoMap.put("foo", info);

        // ## Act ##
        boolean actual = impl.processBoolean("foo", "0", null, 0, columnMetaInfoMap);

        // ## Assert ##
        log("actual=" + actual);
        assertFalse(actual);
    }

    // ===================================================================================
    //                                                                          Skip Sheet
    //                                                                          ==========
    @Test
    public void test_DfXlsDataHandlerImpl_setSkipSheet_SyntaxError() {
        // ## Arrange ##
        final DfXlsDataHandlerImpl impl = new DfXlsDataHandlerImpl();

        // ## Act & Assert ##
        try {
            impl.setSkipSheet("MST.*+`*`+*P*`+*}+");
            fail();
        } catch (IllegalStateException e) {
            // OK
            log(e.getMessage());
            assertNotNull(e.getCause());
            log(e.getCause().getMessage());
            assertTrue(e.getCause() instanceof PatternSyntaxException);
        }
    }
}
