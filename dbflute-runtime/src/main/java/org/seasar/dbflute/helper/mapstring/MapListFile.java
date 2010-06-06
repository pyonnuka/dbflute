/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
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
package org.seasar.dbflute.helper.mapstring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.seasar.dbflute.helper.mapstring.impl.MapListStringImpl;
import org.seasar.dbflute.util.DfSystemUtil;

/**
 * @author jflute
 * @since 0.9.7.1 (2010/06/06 Sunday)
 */
public class MapListFile {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final String UTF8_ENCODING = "UTF-8";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String _fileEncoding;
    protected final String _lineCommentMark;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MapListFile() { // as default
        _fileEncoding = UTF8_ENCODING;
        _lineCommentMark = "#";
    }

    // ===================================================================================
    //                                                                                 Map
    //                                                                                 ===
    // -----------------------------------------------------
    //                                                  Read
    //                                                  ----
    /**
     * Read the map string file. <br />
     * If the type of values is various type, this method is available. <br />
     * A trimmed line that starts with '#' is treated as line comment. <br />
     * This is the most basic method in the map-handling methods.
     * <pre>
     * map:{
     *     ; key1 = string-value1
     *     ; key2 = list:{element1 ; element2 }
     *     ; key3 = map:{key1 = value1 ; key2 = value2 }
     *     ; ... = ...
     * }
     * </pre>
     * @param ins The input stream for DBFlute property file. (NotNull)
     * @return The read map. (NotNull)
     */
    public Map<String, Object> readMap(InputStream ins) {
        final String mapString = readString(ins);
        if (mapString.trim().length() == 0) {
            return newLinkedHashMap();
        }
        final MapListString mapListString = createMapListString();
        return mapListString.generateMap(mapString);
    }

    /**
     * Read the map string file as string value. <br />
     * If the type of all values is string type, this method is available. <br />
     * A trimmed line that starts with '#' is treated as line comment.
     * <pre>
     * ex)
     * map:{
     *     ; key1 = string-value1
     *     ; key2 = string-value2
     *     ; ... = ...
     * }
     * </pre>
     * @param ins The input stream for DBFlute property file. (NotNull)
     * @return The read map whose values is string. (NotNull)
     */
    public Map<String, String> readMapAsStringValue(InputStream ins) {
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        final Map<String, Object> map = readMap(ins);
        final Set<Entry<String, Object>> entrySet = map.entrySet();
        for (Entry<String, Object> entry : entrySet) {
            resultMap.put(entry.getKey(), (String) entry.getValue());
        }
        return resultMap;
    }

    /**
     * Read the map string file as string list value. <br />
     * If the type of all values is string list type, this method is available. <br />
     * A trimmed line that starts with '#' is treated as line comment.
     * <pre>
     * ex)
     * map:{
     *     ; key1 = list:{string-element1 ; string-element2 ; ...}
     *     ; key2 = list:{string-element1 ; string-element2 ; ...}
     *     ; ... = list:{...}
     * }
     * </pre>
     * @param ins The input stream for DBFlute property file. (NotNull)
     * @return The read map whose values is string list. (NotNull)
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> readMapAsStringListValue(InputStream ins) {
        final Map<String, List<String>> resultMap = newLinkedHashMap();
        final Map<String, Object> map = readMap(ins);
        final Set<Entry<String, Object>> entrySet = map.entrySet();
        for (Entry<String, Object> entry : entrySet) {
            resultMap.put(entry.getKey(), (List<String>) entry.getValue());
        }
        return resultMap;
    }

    /**
     * Read the map string file as string map value. <br />
     * If the type of all values is string map type, this method is available. <br />
     * A trimmed line that starts with '#' is treated as line comment.
     * <pre>
     * ex)
     * map:{
     *     ; key1 = map:{string-key1 = string-value1 ; string-key2 = string-value2 }
     *     ; key2 = map:{string-key1 = string-value1 ; string-key2 = string-value2 }
     *     ; ... = map:{...}
     * }
     * </pre>
     * @param ins The input stream for DBFlute property file. (NotNull)
     * @return The read map whose values is string map. (NotNull)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, String>> readMapAsStringMapValue(InputStream ins) {
        final Map<String, Map<String, String>> resultMap = newLinkedHashMap();
        final Map<String, Object> map = readMap(ins);
        final Set<Entry<String, Object>> entrySet = map.entrySet();
        for (Entry<String, Object> entry : entrySet) {
            resultMap.put(entry.getKey(), (Map<String, String>) entry.getValue());
        }
        return resultMap;
    }

    // -----------------------------------------------------
    //                                                 Write
    //                                                 -----
    public void writeMap(OutputStream ous, Map<String, ? extends Object> map) {
        final MapListString mapListString = createMapListString();
        final String mapString = mapListString.buildMapString(map);
        writeString(ous, mapString);
    }

    // ===================================================================================
    //                                                                                List
    //                                                                                ====
    // -----------------------------------------------------
    //                                                  Read
    //                                                  ----
    /**
     * Read the list string file. <br />
     * If the type of values is various type, this method is available. <br />
     * A trimmed line that starts with '#' is treated as line comment. <br />
     * <pre>
     * list:{
     *     ; element1
     *     ; list:{element2-1 ; element2-2 }
     *     ; map:{key3-1 = value3-1 ; key3-2 = value3-2 }
     *     ; ... = ...
     * }
     * </pre>
     * @param ins The input stream for DBFlute property file. (NotNull)
     * @return The read list. (NotNull)
     */
    public List<Object> readList(InputStream ins) {
        final String listString = readString(ins);
        if (listString.trim().length() == 0) {
            return new ArrayList<Object>();
        }
        final MapListString mapListString = createMapListString();
        return mapListString.generateList(listString);
    }

    // ===================================================================================
    //                                                                              String
    //                                                                              ======
    /**
     * Read the string file. <br />
     * A trimmed line that starts with '#' is treated as line comment.
     * @param ins The input stream for DBFlute property file. (NotNull)
     * @return The read string. (NotNull)
     */
    protected String readString(InputStream ins) {
        final String encoding = getFileEncoding();
        final String lineComment = getLineCommentMark();
        final StringBuilder sb = new StringBuilder();
        InputStreamReader ir = null;
        BufferedReader br = null;
        try {
            ir = new InputStreamReader(ins, encoding);
            br = new BufferedReader(ir);

            int count = -1;
            while (true) {
                ++count;

                final String lineString = br.readLine();
                if (lineString == null) {
                    break;
                }
                // if the line is comment, skip reading
                if (lineComment != null && lineString.trim().startsWith(lineComment)) {
                    continue;
                }
                sb.append(lineString + ln());
            }
        } catch (UnsupportedEncodingException e) {
            String msg = "The encoding is unsupported: " + encoding;
            throw new IllegalStateException(msg, e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return removeInitialUnicodeBomIfNeeds(encoding, sb.toString().trim());
    }

    protected void writeString(OutputStream ous, String mapListString) {
        final String encoding = getFileEncoding();
        OutputStreamWriter ow = null;
        BufferedWriter bw = null;
        try {
            ow = new OutputStreamWriter(ous, encoding);
            bw = new BufferedWriter(ow);
            bw.write(removeInitialUnicodeBomIfNeeds(encoding, mapListString));
            bw.flush();
        } catch (UnsupportedEncodingException e) {
            String msg = "The encoding is unsupported: " + encoding;
            throw new IllegalStateException(msg, e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    // ===================================================================================
    //                                                                     Map List String
    //                                                                     ===============
    protected MapListString createMapListString() {
        return new MapListStringImpl();
    }

    // ===================================================================================
    //                                                                       Assist Helper
    //                                                                       =============
    protected String removeInitialUnicodeBomIfNeeds(String encoding, String value) {
        if (UTF8_ENCODING.equalsIgnoreCase(encoding) && value.length() > 0 && value.charAt(0) == '\uFEFF') {
            value = value.substring(1);
        }
        return value;
    }

    // ===================================================================================
    //                                                                      General Helper
    //                                                                      ==============
    protected String ln() {
        return DfSystemUtil.getLineSeparator();
    }

    protected <KEY, VALUE> LinkedHashMap<KEY, VALUE> newLinkedHashMap() {
        return new LinkedHashMap<KEY, VALUE>();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    protected String getFileEncoding() {
        return _fileEncoding;
    }

    protected String getLineCommentMark() {
        return _lineCommentMark;
    }
}