package org.dbflute.bhv.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dbflute.Entity;
import org.dbflute.cbean.ConditionBean;
import org.dbflute.dbmeta.DBMeta;
import org.dbflute.outsidesql.OutsideSqlOption;
import org.dbflute.s2dao.metadata.TnBeanMetaData;
import org.dbflute.s2dao.metadata.PropertyType;


/**
 * @author DBFlute(AutoGenerator)
 */
public abstract class AbstractListEntityCommand extends AbstractBehaviorCommand<int[]> {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The type of entity. (Required) */
    protected Class<? extends Entity> _entityType;

    /** The instance of condition-bean. (Required) */
    protected List<? extends Entity> _entityList;

    // ===================================================================================
    //                                                                   Basic Information
    //                                                                   =================
    public Class<?> getCommandReturnType() {
        return int[].class;
    }

    // ===================================================================================
    //                                                                  Detail Information
    //                                                                  ==================
    public boolean isConditionBean() {
        return false;
    }

    public boolean isOutsideSql() {
        return false;
    }

    public boolean isProcedure() {
        return false;
    }

    public boolean isSelect() {
        return false;
    }

    public boolean isSelectCount() {
        return false;
    }

    // ===================================================================================
    //                                                                             Factory
    //                                                                             =======
    // -----------------------------------------------------
    //                                          BeanMetaData
    //                                          ------------
    protected TnBeanMetaData createBeanMetaData() {
        return _beanMetaDataFactory.createBeanMetaData(_entityType);
    }

    // ===================================================================================
    //                                                                    Process Callback
    //                                                                    ================
    public void beforeGettingSqlExecution() {
    }

    public void afterExecuting() {
    }

    // ===================================================================================
    //                                                               SqlExecution Handling
    //                                                               =====================
    public String buildSqlExecutionKey() {
        assertStatus("buildSqlExecutionKey");
        return _tableDbName + ":" + getCommandName() + "(List<" + _entityType.getSimpleName() + ">)";
    }

    public Object[] getSqlExecutionArgument() {
        assertStatus("getSqlExecutionArgument");
        return new Object[] { _entityList };
    }

    // ===================================================================================
    //                                                                Argument Information
    //                                                                ====================
    public ConditionBean getConditionBean() {
        return null;
    }

    public String getOutsideSqlPath() {
        return null;
    }

    public OutsideSqlOption getOutsideSqlOption() {
        return null;
    }

    // ===================================================================================
    //                                                                       Assist Helper
    //                                                                       =============
    protected DBMeta findDBMeta() {
        // /- - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // Cannot use the handler of DBMeta instance
        // because the customize-entity is contained to find here.
        // - - - - - - - - - -/
        // DBMetaInstanceHandler.findDBMeta(_tableDbName);

        final Class<?> beanType = _entityType;
        if (beanType == null) {
            return null;
        }
        if (!Entity.class.isAssignableFrom(beanType)) {
            return null;
        }
        final Entity entity;
        try {
            entity = (Entity)beanType.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return entity.getDBMeta();
    }

    protected String[] getPersistentPropertyNames(TnBeanMetaData bmd) {
        final List<String> nameList = new ArrayList<String>();
        Map<String, PropertyType> propertyTypeMap = bmd.getPropertyTypeMap();
        Set<String> keySet = propertyTypeMap.keySet();
        for (String key : keySet) {
            PropertyType pt = propertyTypeMap.get(key);
            if (pt.isPersistent()) {
                nameList.add(pt.getPropertyName());
            }
        }
        return nameList.toArray(new String[nameList.size()]);
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertStatus(String methodName) {
        assertBasicProperty(methodName);
        assertComponentProperty(methodName);
        if (_entityType == null) {
            throw new IllegalStateException(buildAssertMessage("_entityType", methodName));
        }
        if (_entityList == null) {
            throw new IllegalStateException(buildAssertMessage("_entityList", methodName));
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public void setEntityType(Class<? extends Entity> entityType) {
        _entityType = entityType;
    }

    public void setEntityList(List<? extends Entity> entityList) {
        _entityList = entityList;
    }
}
