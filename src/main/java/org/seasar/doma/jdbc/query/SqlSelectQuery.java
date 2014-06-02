/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
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
package org.seasar.doma.jdbc.query;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import org.seasar.doma.internal.jdbc.sql.NodePreparedSqlBuilder;
import org.seasar.doma.jdbc.SelectOptionsAccessor;
import org.seasar.doma.jdbc.SqlKind;
import org.seasar.doma.jdbc.SqlNode;

/**
 * @author taedium
 * 
 */
public class SqlSelectQuery extends AbstractSelectQuery {

    protected SqlNode sqlNode;

    @Override
    public void prepare() {
        assertNotNull(sqlNode);
        super.prepare();
    }

    protected void prepareSql() {
        SqlNode transformedSqlNode = config.getDialect()
                .transformSelectSqlNode(sqlNode, options);
        buildSql((evaluator, expander) -> {
            NodePreparedSqlBuilder sqlBuilder = new NodePreparedSqlBuilder(
                    config, SqlKind.SELECT, null, evaluator, sqlLogType,
                    expander);
            return sqlBuilder.build(transformedSqlNode);
        });

    }

    @Override
    public void complete() {
        if (SelectOptionsAccessor.isCount(options)) {
            executeCount(sqlNode);
        }
    }

    public void setSqlNode(SqlNode sqlNode) {
        this.sqlNode = sqlNode;
    }

}
