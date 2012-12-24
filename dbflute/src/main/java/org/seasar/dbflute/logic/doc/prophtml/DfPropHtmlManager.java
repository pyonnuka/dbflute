/*
 * Copyright 2004-2013 the Seasar Foundation and the Others.
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
package org.seasar.dbflute.logic.doc.prophtml;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.seasar.dbflute.DfBuildProperties;
import org.seasar.dbflute.exception.DfIllegalPropertySettingException;
import org.seasar.dbflute.exception.factory.ExceptionMessageBuilder;
import org.seasar.dbflute.helper.io.prop.DfJavaPropertiesProperty;
import org.seasar.dbflute.helper.io.prop.DfJavaPropertiesReader;
import org.seasar.dbflute.helper.io.prop.DfJavaPropertiesResult;
import org.seasar.dbflute.properties.DfDocumentProperties;
import org.seasar.dbflute.util.DfCollectionUtil;
import org.seasar.dbflute.util.Srl;

/**
 * @author jflute
 * @since 1.0.1 (2012/12/21 Friday)
 */
public class DfPropHtmlManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Log _log = LogFactory.getLog(DfPropHtmlManager.class);

    /** The standard environment type. */
    private static final String ENV_TYPE_DEFAULT = "-";

    /** The root for language type. */
    private static final String LANG_TYPE_DEFAULT = "-";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The map of request. map:{requestName = request} (NotNull) */
    protected final Map<String, DfPropHtmlRequest> _requestMap = DfCollectionUtil.newLinkedHashMap();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public DfPropHtmlManager() {
    }

    // ===================================================================================
    //                                                                        Load Request
    //                                                                        ============
    public void loadRequest() {
        final DfDocumentProperties prop = getDocumentProperties();
        final Map<String, Map<String, Object>> propertiesHtmlMap = prop.getPropertiesHtmlMap();
        if (propertiesHtmlMap.isEmpty()) {
            return;
        }
        for (Entry<String, Map<String, Object>> entry : propertiesHtmlMap.entrySet()) {
            final String requestName = entry.getKey();
            _log.info("...Loading properties HTML request: " + requestName);
            final Map<String, Object> requestMap = entry.getValue();
            final DfPropHtmlRequest request = prepareRequest(requestMap, requestName);
            _requestMap.put(requestName, request);
        }
        analyzePropertiesDiff();
    }

    protected DfPropHtmlRequest prepareRequest(Map<String, Object> requestMap, String requestName) {
        final DfPropHtmlRequest request = new DfPropHtmlRequest(requestName);
        final DfDocumentProperties prop = getDocumentProperties();
        final String rootFile = prop.getPropertiesHtmlResourceRootFile(requestMap);
        final Map<String, DfPropHtmlFileAttribute> defaultEnvMap = setupDefaultEnvProperty(request, rootFile);
        assertPropHtmlRootFileExists(defaultEnvMap, requestName, rootFile);
        final Map<String, String> environmentMap = prop.getPropertiesHtmlResourceEnvironmentMap(requestMap);
        final String standardPureFileName = Srl.substringLastRear(rootFile, "/");
        for (Entry<String, String> envEntry : environmentMap.entrySet()) {
            final String envType = envEntry.getKey();
            final String envDir = envEntry.getValue();
            final String envFile = envDir + "/" + standardPureFileName;
            setupEnvironmentProperty(request, envFile, envType, defaultEnvMap);
        }
        final List<String> ignoredKeyList = prop.getPropertiesHtmlDiffIgnoredKeyList(requestMap);
        request.addDiffIgnoredKeyAll(ignoredKeyList);
        return request;
    }

    protected void assertPropHtmlRootFileExists(Map<String, DfPropHtmlFileAttribute> defaultEnvMap, String requestName,
            String rootFile) {
        if (!defaultEnvMap.isEmpty()) {
            return;
        }
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the root file for PropertiesHtml.");
        br.addItem("Request Name");
        br.addElement(requestName);
        br.addItem("Root File");
        br.addElement(rootFile);
        final String msg = br.buildExceptionMessage();
        throw new DfIllegalPropertySettingException(msg);
    }

    // ===================================================================================
    //                                                                     Set up Property
    //                                                                     ===============
    protected Map<String, DfPropHtmlFileAttribute> setupDefaultEnvProperty(DfPropHtmlRequest request,
            String propertiesFile) {
        return doSetupEnvironmentProperty(request, propertiesFile, ENV_TYPE_DEFAULT, null);
    }

    protected Map<String, DfPropHtmlFileAttribute> setupEnvironmentProperty(DfPropHtmlRequest request,
            String propertiesFile, String envType, Map<String, DfPropHtmlFileAttribute> defaultEnvMap) {
        return doSetupEnvironmentProperty(request, propertiesFile, envType, defaultEnvMap);
    }

    protected Map<String, DfPropHtmlFileAttribute> doSetupEnvironmentProperty(DfPropHtmlRequest request,
            String propertiesFile, String envType, Map<String, DfPropHtmlFileAttribute> defaultEnvMap) {
        final DfJavaPropertiesReader reader = new DfJavaPropertiesReader();
        final List<File> familyFileList = extractFamilyFileList(request.getRequestName(), propertiesFile);
        if (familyFileList.isEmpty()) {
            return DfCollectionUtil.emptyMap();
        }
        final String specifiedPureFileName = Srl.substringLastRear(propertiesFile, "/");
        final Map<String, DfPropHtmlFileAttribute> attributeMap = DfCollectionUtil.newLinkedHashMap();
        DfPropHtmlFileAttribute rootAttribute = null;
        if (defaultEnvMap != null) { // when specified environment
            for (DfPropHtmlFileAttribute attribute : defaultEnvMap.values()) {
                if (attribute.isRootFile()) { // always exists here
                    rootAttribute = attribute;
                }
            }
        }
        for (File familyFile : familyFileList) {
            final String langType = extractLangType(familyFile.getName());
            final String fileKey = "" + envType + ":" + familyFile.getName();
            _log.info("...Reading properties file: " + fileKey);
            final DfJavaPropertiesResult jpropResult = reader.read(familyFile, "UTF-8");
            final List<DfJavaPropertiesProperty> jpropList = jpropResult.getPropertyList();
            final Set<String> propertyKeySet = DfCollectionUtil.newLinkedHashSet();
            for (DfJavaPropertiesProperty jprop : jpropList) {
                final String propertyKey = jprop.getPropertyKey();
                final String propertyValue = jprop.getPropertyValue();
                final String comment = jprop.getComment();
                request.addProperty(propertyKey, envType, langType, propertyValue, comment);
                propertyKeySet.add(propertyKey);
            }
            final DfPropHtmlFileAttribute attribute = new DfPropHtmlFileAttribute(familyFile, envType, langType);
            if (defaultEnvMap != null) { // when specified environment
                if (rootAttribute != null) { // always true
                    // every files compare with root file here
                    attribute.setStandardAttribute(rootAttribute);
                } else { // no way but just in case
                    final DfPropHtmlFileAttribute standardAttribute = defaultEnvMap.get(langType);
                    if (standardAttribute != null) {
                        // same language on standard environment is my standard here
                        attribute.setStandardAttribute(standardAttribute);
                    }
                    // if standard not found, the file exists only in the environment
                }
            } else { // when default environment
                attribute.toBeDefaultEnv();
                if (familyFile.getName().equals(specifiedPureFileName)) {
                    attribute.toBeRootFile();
                    rootAttribute = attribute; // save for relation to root
                }
            }
            attribute.setKeyCount(jpropList.size());
            attribute.addDuplicateKeyAll(jpropResult.getDuplicateKeyList());
            attribute.addPropertyKeyAll(propertyKeySet);
            request.addFileAttribute(attribute);
            attributeMap.put(langType, attribute);
        }
        if (rootAttribute != null) {
            for (DfPropHtmlFileAttribute attribute : attributeMap.values()) {
                if (attribute.isDefaultEnv() && !attribute.isRootFile()) {
                    attribute.setStandardAttribute(rootAttribute);
                }
            }
        }
        return attributeMap;
    }

    protected List<File> extractFamilyFileList(String requestName, String propertiesFile) {
        final List<File> familyFileList = DfCollectionUtil.newArrayList();
        final File targetDir = new File(Srl.substringLastFront(propertiesFile, "/"));
        final String pureFileName = Srl.substringLastRear(propertiesFile, "/");
        final String pureFileNameNoExt = Srl.substringLastFront(pureFileName, ".");
        final String ext = Srl.substringLastRear(pureFileName, ".");
        final String pureFileNameNoExtNoLang;
        final String langType = extractLangType(propertiesFile);
        if (!LANG_TYPE_DEFAULT.equals(langType)) { // the properties file has language type
            pureFileNameNoExtNoLang = Srl.substringLastFront(pureFileNameNoExt, "_");
        } else {
            pureFileNameNoExtNoLang = pureFileNameNoExt;
        }
        assertPropHtmlEnvironmentDirectoryExists(targetDir, requestName, propertiesFile);
        final File[] listFiles = targetDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return false;
                }
                final String pureName = file.getName();
                return pureName.startsWith(pureFileNameNoExtNoLang) && pureName.endsWith("." + ext);
            }
        });
        if (listFiles != null && listFiles.length > 0) {
            for (File file : listFiles) {
                familyFileList.add(file);
            }
        }
        return familyFileList;
    }

    protected String extractLangType(String propertiesFile) {
        final String pureFileName = Srl.substringLastRear(propertiesFile, "/");
        final String pureFileNameNoExt = Srl.substringLastFront(pureFileName, ".");
        if (pureFileNameNoExt.contains("_")) {
            final String langType = Srl.substringLastRear(pureFileNameNoExt, "_");
            if (langType.length() == 2) {
                return langType; // e.g. ja, en
            }
        }
        return LANG_TYPE_DEFAULT; // as default
    }

    protected void assertPropHtmlEnvironmentDirectoryExists(File targetDir, String requestName, String propertiesFile) {
        if (targetDir.exists()) {
            return;
        }
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the directory for the file for PropertiesHtml.");
        br.addItem("Request Name");
        br.addElement(requestName);
        br.addItem("Properties File");
        br.addElement(propertiesFile);
        final String msg = br.buildExceptionMessage();
        throw new DfIllegalPropertySettingException(msg);
    }

    // ===================================================================================
    //                                                                  Analyze Difference
    //                                                                  ==================
    protected void analyzePropertiesDiff() {
        for (Entry<String, DfPropHtmlRequest> entry : _requestMap.entrySet()) {
            final DfPropHtmlRequest request = entry.getValue();
            final Set<String> ignoredSet = DfCollectionUtil.newHashSet(request.getDiffIgnoredKeyList());
            final List<DfPropHtmlFileAttribute> attributeList = request.getFileAttributeList();
            for (DfPropHtmlFileAttribute attribute : attributeList) {
                if (attribute.isRootFile()) {
                    continue;
                }
                final DfPropHtmlFileAttribute standardAttribute = attribute.getStandardAttribute();
                if (standardAttribute == null) {
                    attribute.toBeLonely();
                    continue;
                }
                final Set<String> standardPropertyKeySet = standardAttribute.getPropertyKeySet();
                final Set<String> propertyKeySet = attribute.getPropertyKeySet();
                for (String propertyKey : propertyKeySet) {
                    if (ignoredSet.contains(propertyKey)) {
                        continue;
                    }
                    if (!standardPropertyKeySet.contains(propertyKey)) {
                        attribute.addOverKey(propertyKey);
                    }
                }
                for (String standardPropertyKey : standardPropertyKeySet) {
                    if (ignoredSet.contains(standardPropertyKey)) {
                        continue;
                    }
                    if (!propertyKeySet.contains(standardPropertyKey)) {
                        attribute.addShortKey(standardPropertyKey);
                    }
                }
            }
        }
    }

    // ===================================================================================
    //                                                                          Properties
    //                                                                          ==========
    protected DfBuildProperties getProperties() {
        return DfBuildProperties.getInstance();
    }

    protected DfDocumentProperties getDocumentProperties() {
        return getProperties().getDocumentProperties();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public List<DfPropHtmlRequest> getRequestList() {
        return DfCollectionUtil.newArrayList(_requestMap.values());
    }

    public Map<String, DfPropHtmlRequest> getRequestMap() {
        return _requestMap;
    }
}
