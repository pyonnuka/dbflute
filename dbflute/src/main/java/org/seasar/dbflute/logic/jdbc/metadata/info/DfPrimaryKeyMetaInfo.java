/*
 * Copyright 2004-2008 the Seasar Foundation and the Others.
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
package org.seasar.dbflute.logic.jdbc.metadata.info;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jflute
 */
public class DfPrimaryKeyMetaInfo {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected String _primaryKeyName;

    protected List<String> _primaryKeyList = new ArrayList<String>();

    // ===================================================================================
    //                                                                         Easy-to-Use
    //                                                                         ===========
    public boolean hasPrimaryKeyName() {
        return _primaryKeyName != null && _primaryKeyName.trim().length() > 0;
    }

    public boolean containsColumn(String columnName) {
        return _primaryKeyList.contains(columnName);
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return _primaryKeyName + _primaryKeyList;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getPrimaryKeyName() {
        return _primaryKeyName;
    }

    public void setPrimaryKeyName(String primaryKeyName) {
        _primaryKeyName = primaryKeyName;
    }

    public List<String> getPrimaryKeyList() {
        return _primaryKeyList;
    }

    public void addPrimaryKeyList(String primaryKey) {
        _primaryKeyList.add(primaryKey);
    }
}