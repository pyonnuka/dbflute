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
package org.seasar.dbflute.s2dao.sqlcommand;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.seasar.dbflute.bhv.core.SqlExecution;
import org.seasar.dbflute.jdbc.StatementFactory;
import org.seasar.dbflute.outsidesql.OutsideSqlContext;
import org.seasar.dbflute.outsidesql.ProcedurePmb;
import org.seasar.dbflute.s2dao.jdbc.TnResultSetHandler;
import org.seasar.dbflute.s2dao.metadata.TnProcedureMetaData;
import org.seasar.dbflute.s2dao.metadata.TnProcedureParameterType;
import org.seasar.dbflute.s2dao.sqlhandler.TnProcedureHandler;
import org.seasar.dbflute.s2dao.sqlhandler.TnProcedureHandler.TnProcedureResultSetHandlerProvider;

/**
 * {Refers to Seasar and Extends its class}
 * @author jflute
 */
public class TnProcedureCommand implements TnSqlCommand, SqlExecution {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected DataSource _dataSource;
    protected StatementFactory _statementFactory;
    protected TnProcedureMetaData _procedureMetaData;
    protected TnProcedureResultSetHandlerFactory _procedureResultSetHandlerFactory;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TnProcedureCommand(DataSource dataSource, StatementFactory statementFactory,
            TnProcedureMetaData procedureMetaData, TnProcedureResultSetHandlerFactory procedureResultSetHandlerFactory) {
        this._dataSource = dataSource;
        this._statementFactory = statementFactory;
        this._procedureMetaData = procedureMetaData;
        this._procedureResultSetHandlerFactory = procedureResultSetHandlerFactory;
    }

    public static interface TnProcedureResultSetHandlerFactory { // is needed to construct an instance
        TnResultSetHandler createBeanHandler(Class<?> beanClass);

        TnResultSetHandler createMapHandler();
    }

    // ===================================================================================
    //                                                                             Execute
    //                                                                             =======
    public Object execute(final Object[] args) {
        // the args is unused because of getting from context
        // (actually the args has same parameter as context)

        final OutsideSqlContext outsideSqlContext = OutsideSqlContext.getOutsideSqlContextOnThread();
        final Object pmb = outsideSqlContext.getParameterBean(); // basically implements ProcedurePmb
        final TnProcedureHandler handler = createProcedureHandler(pmb);
        final Object[] onlyPmbArgs = new Object[] { pmb };

        // The method that builds display SQL is overridden for procedure
        // so it can set arguments which have only parameter bean
        handler.setExceptionMessageSqlArgs(onlyPmbArgs);

        return handler.execute(onlyPmbArgs);
    }

    protected TnProcedureHandler createProcedureHandler(Object pmb) {
        final String sql = buildSql(pmb);
        return new TnProcedureHandler(_dataSource, sql, _statementFactory, _procedureMetaData,
                createProcedureResultSetHandlerFactory());
    }

    protected String buildSql(Object pmb) {
        final String procedureName = _procedureMetaData.getProcedureName();
        final int bindSize = _procedureMetaData.getBindParameterTypeList().size();
        final boolean existsReturn = _procedureMetaData.hasReturnParameterType();

        // default is that escape is valid
        // but basically pmb is ProcedurePmb here
        boolean kakou = true;
        if (pmb instanceof ProcedurePmb) { // so you can specify through ProcedurePmb
            kakou = ((ProcedurePmb) pmb).isEscapeStatement();
        }
        return doBuildSql(procedureName, bindSize, existsReturn, kakou);
    }

    protected String doBuildSql(String procedureName, int bindSize, boolean existsReturn, boolean kakou) {
        final StringBuilder sb = new StringBuilder();
        final int argSize;
        {
            if (existsReturn) {
                sb.append("? = ");
                argSize = bindSize - 1;
            } else {
                argSize = bindSize;
            }
        }
        sb.append("call ").append(procedureName).append("(");
        for (int i = 0; i < argSize; i++) {
            sb.append("?, ");
        }
        if (argSize > 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(")");
        if (kakou) {
            sb.insert(0, "{").append("}");
        }
        return sb.toString();
    }

    protected TnProcedureResultSetHandlerProvider createProcedureResultSetHandlerFactory() {
        return new TnProcedureResultSetHandlerProvider() {
            public TnResultSetHandler provideResultSetHandler(TnProcedureParameterType ppt, ResultSet rs) {
                final Class<?> parameterType = ppt.getParameterType();
                if (!List.class.isAssignableFrom(parameterType)) {
                    String msg = "The parameter type for result set should be List:";
                    msg = msg + " parameter=" + ppt.getParameterName() + " type=" + parameterType;
                    throw new IllegalStateException(msg);
                }
                final Class<?> elementType = ppt.getElementType();
                if (elementType == null) {
                    String msg = "The parameter type for result set should have generic type of List:";
                    msg = msg + " parameter=" + ppt.getParameterName() + " type=" + parameterType;
                    throw new IllegalStateException(msg);
                }
                if (Map.class.isAssignableFrom(elementType)) {
                    return _procedureResultSetHandlerFactory.createMapHandler();
                } else {
                    return _procedureResultSetHandlerFactory.createBeanHandler(elementType);
                }
            }
        };
    }
}
