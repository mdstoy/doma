package org.seasar.doma.jdbc.query;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.seasar.doma.internal.expr.ExpressionEvaluator;
import org.seasar.doma.internal.expr.Value;
import org.seasar.doma.internal.jdbc.sql.NodePreparedSqlBuilder;
import org.seasar.doma.internal.jdbc.sql.SqlContext;
import org.seasar.doma.internal.jdbc.sql.node.ExpandNode;
import org.seasar.doma.internal.jdbc.sql.node.PopulateNode;
import org.seasar.doma.jdbc.PreparedSql;
import org.seasar.doma.jdbc.SqlExecutionSkipCause;
import org.seasar.doma.jdbc.SqlKind;
import org.seasar.doma.jdbc.SqlLogType;
import org.seasar.doma.jdbc.entity.EntityDesc;

public abstract class SqlTemplateModifyQuery extends AbstractQuery implements ModifyQuery {

  protected static final String[] EMPTY_STRINGS = new String[] {};

  protected final SqlKind kind;

  protected final Map<String, Value> parameters = new LinkedHashMap<>();

  protected PreparedSql sql;

  protected boolean optimisticLockCheckRequired;

  protected SqlLogType sqlLogType;

  protected String[] includedPropertyNames = EMPTY_STRINGS;

  protected String[] excludedPropertyNames = EMPTY_STRINGS;

  protected boolean executable;

  protected SqlExecutionSkipCause sqlExecutionSkipCause = SqlExecutionSkipCause.STATE_UNCHANGED;

  protected SqlTemplateModifyQuery(SqlKind kind) {
    assertNotNull(kind);
    this.kind = kind;
  }

  protected void prepareOptions() {
    if (queryTimeout <= 0) {
      queryTimeout = config.getQueryTimeout();
    }
  }

  protected void prepareSql() {
    var sqlTemplate = config.getSqlTemplateRepository().getSqlTemplate(method, config.getDialect());
    var evaluator =
        new ExpressionEvaluator(
            parameters, config.getDialect().getExpressionFunctions(), config.getClassHelper());
    var sqlBuilder =
        new NodePreparedSqlBuilder(
            config,
            kind,
            sqlTemplate.getPath(),
            evaluator,
            sqlLogType,
            this::expandColumns,
            this::populateValues);
    sql = sqlBuilder.build(sqlTemplate.getSqlNode(), this::comment);
  }

  protected List<String> expandColumns(ExpandNode node) {
    throw new UnsupportedOperationException();
  }

  protected void populateValues(PopulateNode node, SqlContext context) {
    throw new UnsupportedOperationException();
  }

  public void addParameter(String name, Class<?> type, Object value) {
    assertNotNull(name, type);
    addParameterInternal(name, type, value);
  }

  public void addParameterInternal(String name, Class<?> type, Object value) {
    parameters.put(name, new Value(type, value));
  }

  public void setSqlLogType(SqlLogType sqlLogType) {
    this.sqlLogType = sqlLogType;
  }

  public void setIncludedPropertyNames(String... includedPropertyNames) {
    this.includedPropertyNames = includedPropertyNames;
  }

  public void setExcludedPropertyNames(String... excludedPropertyNames) {
    this.excludedPropertyNames = excludedPropertyNames;
  }

  @Override
  public PreparedSql getSql() {
    return sql;
  }

  @Override
  public boolean isOptimisticLockCheckRequired() {
    return optimisticLockCheckRequired;
  }

  @Override
  public boolean isExecutable() {
    return executable;
  }

  @Override
  public SqlExecutionSkipCause getSqlExecutionSkipCause() {
    return sqlExecutionSkipCause;
  }

  @Override
  public boolean isAutoGeneratedKeysSupported() {
    return false;
  }

  @Override
  public SqlLogType getSqlLogType() {
    return sqlLogType;
  }

  public abstract <E> void setEntityAndEntityDesc(String name, E entity, EntityDesc<E> entityDesc);

  @Override
  public String toString() {
    return sql != null ? sql.toString() : null;
  }
}