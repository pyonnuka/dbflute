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
package org.seasar.dbflute.dbmeta.info;

import org.seasar.dbflute.dbmeta.DBMeta;
import org.seasar.dbflute.dbmeta.DBMeta.OptimisticLockType;

/**
 * The information of column.
 * @author jflute
 */
public class ColumnInfo {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final DBMeta _dbmeta;
    protected final String _columnDbName;
    protected final String _columnAlias;
    protected final boolean _notNull;
    protected final String _propertyName;
    protected final Class<?> _propertyType;
    protected final boolean _primary;
    protected final boolean _autoIncrement;
    protected final String _columnDbType;
    protected final Integer _columnSize;
    protected final Integer _columnDecimalDigits;
    protected final boolean _commonColumn;
    protected final OptimisticLockType _optimisticLockType;
    protected final String _columnComment;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ColumnInfo(DBMeta dbmeta, String columnDbName, String columnAlias, boolean notNull, String propertyName,
            Class<?> propertyType, boolean primary, boolean autoIncrement, String columnDbType, Integer columnSize,
            Integer columnDecimalDigits, boolean commonColumn, OptimisticLockType optimisticLockType,
            String columnComment) {
        assertObjectNotNull("dbmeta", dbmeta);
        assertObjectNotNull("columnDbName", columnDbName);
        assertObjectNotNull("propertyName", propertyName);
        assertObjectNotNull("propertyType", propertyType);
        this._dbmeta = dbmeta;
        this._columnDbName = columnDbName;
        this._columnAlias = columnAlias;
        this._notNull = notNull;
        this._propertyName = propertyName;
        this._propertyType = propertyType;
        this._primary = primary;
        this._autoIncrement = autoIncrement;
        this._columnSize = columnSize;
        this._columnDbType = columnDbType;
        this._columnDecimalDigits = columnDecimalDigits;
        this._commonColumn = commonColumn;
        this._optimisticLockType = optimisticLockType != null ? optimisticLockType : OptimisticLockType.NONE;
        this._columnComment = columnComment;
    }

    // ===================================================================================
    //                                                                              Finder
    //                                                                              ======
    public java.lang.reflect.Method findSetter() {
        return findMethod(_dbmeta.getEntityType(), "set" + buildInitCapPropertyName(),
                new Class<?>[] { this._propertyType });
    }

    public java.lang.reflect.Method findGetter() {
        return findMethod(_dbmeta.getEntityType(), "get" + buildInitCapPropertyName(), new Class<?>[] {});
    }

    protected String buildInitCapPropertyName() {
        return initCap(this._propertyName);
    }

    // ===================================================================================
    //                                                                      General Helper
    //                                                                      ==============
    protected String initCap(final String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    protected java.lang.reflect.Method findMethod(Class<?> clazz, String methodName, Class<?>[] argTypes) {
        try {
            return clazz.getMethod(methodName, argTypes);
        } catch (NoSuchMethodException ex) {
            String msg = "class=" + clazz + " method=" + methodName + "-" + java.util.Arrays.asList(argTypes);
            throw new RuntimeException(msg, ex);
        }
    }

    /**
     * Assert that the object is not null.
     * @param variableName Variable name. (NotNull)
     * @param value Value. (NotNull)
     * @exception IllegalArgumentException
     */
    protected void assertObjectNotNull(String variableName, Object value) {
        if (variableName == null) {
            String msg = "The value should not be null: variableName=null value=" + value;
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "The value should not be null: variableName=" + variableName;
            throw new IllegalArgumentException(msg);
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    public int hashCode() {
        return _dbmeta.hashCode() + _columnDbName.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ColumnInfo)) {
            return false;
        }
        final ColumnInfo target = (ColumnInfo) obj;
        if (!this._dbmeta.equals(target.getDBMeta())) {
            return false;
        }
        if (!this._columnDbName.equals(target.getColumnDbName())) {
            return false;
        }
        return true;
    }

    public String toString() {
        return _dbmeta.getTableDbName() + "." + _columnDbName;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public DBMeta getDBMeta() {
        return _dbmeta;
    }

    /**
     * Get the DB name of the column.
     * @return The DB name of the column. (NotNull)
     */
    public String getColumnDbName() {
        return this._columnDbName;
    }

    /**
     * Get the alias of the column.
     * @return The alias of the column. (Nullable: when it cannot get an alias from meta)
     */
    public String getColumnAlias() {
        return this._columnAlias;
    }

    /**
     * Is the column not null?
     * @return Determination.
     */
    public boolean isNotNull() {
        return this._notNull;
    }

    /**
     * Get the name of property for the column. (JavaBeansRule)
     * @return The name of property for the column. (NotNull)
     */
    public String getPropertyName() {
        return this._propertyName;
    }

    /**
     * Get the type of property for the column.
     * @return The type of property for the column. (NotNull)
     */
    public Class<?> getPropertyType() {
        return this._propertyType;
    }

    /**
     * Is the column a part of primary keys?
     * @return Determination.
     */
    public boolean isPrimary() {
        return this._primary;
    }

    /**
     * Is the column auto increment?
     * @return Determination.
     */
    public boolean isAutoIncrement() {
        return this._autoIncrement;
    }

    /**
     * Get the DB type of the column.
     * @return The DB type of the column. (NotNull: If the type is unknown, it returns 'UnknownType'.)
     */
    public String getColumnDbType() {
        return this._columnDbType;
    }

    /**
     * Get the size of the column.
     * @return The size of the column. (Nullable: If the type does not have size, it returns null.)
     */
    public Integer getColumnSize() {
        return this._columnSize;
    }

    /**
     * Get the decimal digits of the column.
     * @return The decimal digits of the column. (Nullable: If the type does not have disits, it returns null.)
     */
    public Integer getColumnDecimalDigits() {
        return this._columnDecimalDigits;
    }

    /**
     * Is the column a part of common columns?
     * @return Determination.
     */
    public boolean isCommonColumn() {
        return this._commonColumn;
    }

    /**
     * Is the column for optimistic lock?
     * @return Determination.
     */
    public boolean isOptimisticLock() {
        return isVersionNo() || isUpdateDate();
    }

    /**
     * Is the column version-no for optimistic lock?
     * @return Determination.
     */
    public boolean isVersionNo() {
        return OptimisticLockType.VERSION_NO == _optimisticLockType;
    }

    /**
     * Is the column update-date for optimistic lock?
     * @return Determination.
     */
    public boolean isUpdateDate() {
        return OptimisticLockType.UPDATE_DATE == _optimisticLockType;
    }

    /**
     * Get the comment of the column. <br />
     * If the real comment contains the alias,
     * this result does NOT contain it and its delimiter.  
     * @return The comment of the column. (Nullable: when it cannot get an alias from meta)
     */
    public String getColumnComment() {
        return this._columnComment;
    }
}
