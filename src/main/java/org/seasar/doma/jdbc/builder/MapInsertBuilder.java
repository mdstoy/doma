package org.seasar.doma.jdbc.builder;

import java.sql.Statement;
import java.util.Map;
import java.util.stream.Collectors;
import org.seasar.doma.DomaIllegalArgumentException;
import org.seasar.doma.DomaNullPointerException;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.JdbcException;
import org.seasar.doma.jdbc.Sql;
import org.seasar.doma.jdbc.SqlLogType;
import org.seasar.doma.jdbc.UniqueConstraintException;

/**
 * A builder that builds an SQL INSERT statement from a map.
 *
 * <p>This is not thread safe.
 *
 * <h2>Java</h2>
 *
 * <pre>
 * MapInsertBuilder builder = MapInsertBuilder.newInstance(config, "Emp");
 * builder.execute(new LinkedHashMap&lt;String, Object&gt;(){{
 *   put("name", "SMITH");
 *   put("salary", 1000)
 * }});
 * </pre>
 *
 * <h2>built SQL</h2>
 *
 * <pre>
 * insert into Emp
 * (name, salary)
 * values('SMITH', 1000)
 * </pre>
 *
 * @author bakenezumi
 */
public class MapInsertBuilder {

  private final InsertBuilder builder;

  private final String tableName;

  private MapInsertBuilder(Config config, String tableName) {
    this.builder = InsertBuilder.newInstance(config);
    builder.callerClassName(getClass().getName());
    this.tableName = tableName;
  }

  /**
   * ファクトリメソッドです。
   *
   * @param config 設定
   * @param tableName テーブル名
   * @return INSERT文を組み立てるビルダー
   * @throws DomaNullPointerException 引数が{@code null} の場合
   */
  public static MapInsertBuilder newInstance(Config config, String tableName) {
    if (config == null) {
      throw new DomaNullPointerException("config");
    }
    if (tableName == null) {
      throw new DomaNullPointerException("tableName");
    }
    return new MapInsertBuilder(config, tableName);
  }

  /**
   * パラメータからINSERT文を組み立てて実行します。
   *
   * @return 更新件数
   * @param parameter INSERT文の生成元となるMap
   * @throws DomaNullPointerException parameterがnullの場合
   * @throws DomaIllegalArgumentException parameterが空の場合
   * @throws UniqueConstraintException 一意制約違反が発生した場合
   * @throws JdbcException 上記以外でJDBCに関する例外が発生した場合
   */
  public int execute(Map<String, Object> parameter) {
    if (parameter == null) {
      throw new DomaNullPointerException("parameter");
    }
    if (parameter.size() < 1) {
      throw new DomaIllegalArgumentException("parameter", "parameter.size() < 1");
    }
    builder
        .sql("insert into ")
        .sql(tableName)
        .sql(" (")
        .sql(parameter.keySet().stream().collect(Collectors.joining(", ")))
        .sql(")");
    builder.sql("values (");
    parameter.forEach(
        (key, value) -> {
          if (value == null) {
            builder.sql("NULL").sql(", ");
          } else {
            // 静的な型指定が行えないためObjectにキャストしている
            // BatchBuilder内で下記clazzを利用した型チェックが行われているため安全である
            @SuppressWarnings("unchecked")
            final Class<Object> clazz = (Class<Object>) value.getClass();
            builder.param(clazz, value).sql(", ");
          }
        });
    builder.removeLast().sql(")");
    return builder.execute();
  }

  /**
   * クエリタイムアウト（秒）を設定します。
   *
   * <p>指定しない場合、 {@link Config#getQueryTimeout()} が使用されます。
   *
   * @param queryTimeout クエリタイムアウト（秒）
   * @see Statement#setQueryTimeout(int)
   */
  public void queryTimeout(int queryTimeout) {
    builder.queryTimeout(queryTimeout);
  }

  /**
   * SQLのログの出力形式を設定します。
   *
   * @param sqlLogType SQLのログの出力形式
   */
  public void sqlLogType(SqlLogType sqlLogType) {
    builder.sqlLogType(sqlLogType);
  }

  /**
   * 呼び出し元のクラス名です。
   *
   * <p>指定しない場合このクラスの名前が使用されます。
   *
   * @param className 呼び出し元のクラス名
   * @throws DomaNullPointerException 引数が {@code null} の場合
   */
  public void callerClassName(String className) {
    builder.callerClassName(className);
  }

  /**
   * 呼び出し元のメソッド名です。
   *
   * <p>指定しない場合このSQLを生成するメソッド（{@link #execute})）の名前が使用されます。
   *
   * @param methodName 呼び出し元のメソッド名
   * @throws DomaNullPointerException 引数が {@code null} の場合
   */
  public void callerMethodName(String methodName) {
    if (methodName == null) {
      throw new DomaNullPointerException("methodName");
    }
    builder.callerMethodName(methodName);
  }

  /**
   * 組み立てられたSQLを返します。
   *
   * @return 組み立てられたSQL
   */
  public Sql<?> getSql() {
    return builder.getSql();
  }
}
