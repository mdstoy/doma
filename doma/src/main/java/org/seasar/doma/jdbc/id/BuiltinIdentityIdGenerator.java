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
package org.seasar.doma.jdbc.id;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.seasar.doma.GenerationType;
import org.seasar.doma.jdbc.JdbcException;
import org.seasar.doma.jdbc.Sql;
import org.seasar.doma.message.MessageCode;

/**
 * @author taedium
 * 
 */
public class BuiltinIdentityIdGenerator extends AbstractIdGenerator implements
        IdentityIdGenerator {

    @Override
    public boolean supportsBatch(IdGenerationConfig config) {
        return false;
    }

    @Override
    public boolean includesIdentityColumn(IdGenerationConfig config) {
        return config.getDialect().includesIdentityColumn();
    }

    @Override
    public boolean supportsAutoGeneratedKeys(IdGenerationConfig config) {
        return config.getDialect().supportsAutoGeneratedKeys();
    }

    @Override
    public Long generatePreInsert(IdGenerationConfig config) {
        return null;
    }

    @Override
    public Long generatePostInsert(IdGenerationConfig config,
            Statement statement) {
        if (config.getDialect().supportsAutoGeneratedKeys()) {
            return getGeneratedValue(config, statement);
        }
        return getGeneratedValue(config);
    }

    protected long getGeneratedValue(IdGenerationConfig context,
            Statement statement) {
        try {
            final ResultSet resultSet = statement.getGeneratedKeys();
            return getGeneratedValue(context, resultSet);
        } catch (final SQLException e) {
            throw new JdbcException(MessageCode.DOMA2018, e, context
                    .getEntity().__getName(), e);
        }
    }

    protected long getGeneratedValue(IdGenerationConfig config) {
        String qualifiedTableName = config.getQualifiedTableName();
        String idColumnName = config.getIdColumnName();
        Sql<?> sql = config.getDialect()
                .getIdentitySelectSql(qualifiedTableName, idColumnName);
        return getGeneratedValue(config, sql);
    }

    @Override
    public GenerationType getGenerationType() {
        return GenerationType.IDENTITY;
    }

}