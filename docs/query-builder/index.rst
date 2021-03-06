============
クエリビルダ
============

.. contents:: 目次
   :depth: 3

``org.seasar.doma.jdbc.builder`` パッケージでは、
プログラムでSQLを組み立てるためのクエリビルダを提供しています。

何らかの理由により :doc:`../query/index` の利用が難しい場合にのみ、
クエリビルダを利用することを推奨します。
また、クエリビルダは :ref:`dao-default-method` の中で使用することを推奨します。

どのクエリビルダも、インスタンスは ``Config`` 型の引数をとる
``static`` な ``newInstance`` メソッドで生成できます。
インスタンスには、 ``sql`` メソッドでSQL文字列の断片を、
``param`` メソッドと ``literal`` メソッドでパラメータの型とパラメータを渡せます。

``param`` メソッドで渡されたパラメータは ``PreparedStatement`` のバインド変数として扱われます。

``literal`` メソッドで渡されたパラメータはSQLにリテラルとして埋め込まれます。
このメソッドでパラメータが渡された場合、SQLインジェクション対策としてのエスケープ処理は実施されません。
しかし、SQLインジェクションを防ぐためにパラメータの値にシングルクォテーションを含めることは禁止しています。

検索においてパラメータのリストを渡す場合は ``params`` メソッドと ``literals`` メソッドを利用できます。
パラメータはカンマで連結されたSQLに変換されます。
これらは、IN句と一緒に利用されることを想定されたメソッドです。

検索
====

検索には、 ``SelectBuilder`` クラスを使用します。

利用例は次のとおりです。

.. code-block:: java

  SelectBuilder builder = SelectBuilder.newInstance(config);
  builder.sql("select");
  builder.sql("id").sql(",");
  builder.sql("name").sql(",");
  builder.sql("salary");
  builder.sql("from Emp");
  builder.sql("where");
  builder.sql("job_type = ").literal(String.class, "fulltime");
  builder.sql("and");
  builder.sql("name like ").param(String.class, "S%");
  builder.sql("and");
  builder.sql("age in (").params(Integer.class, Arrays.asList(20, 30, 40)).sql(")");
  List<Emp> employees = builder.getEntityResultList(Emp.class);

組み立てたSQLのいくつかの方法で取得できます。

1件取得
-------

* getScalarSingleResult
* getOptionalScalarSingleResult
* getEntitySingleResult
* getOptionalEntitySingleResult
* getMapSingleResult
* getOptionalMapSingleResult

複数件取得
----------

* getScalarResultList
* getOptionalScalarResultList
* getEntityResultList
* getMapResultList

イテレート検索
--------------

* iterateAsScalar
* iterateAsOptionalScalar
* iterateAsEntity
* iterateAsMap

ストリーム検索
--------------

* streamAsScalar
* streamAsOptionalScalar
* streamAsEntity
* streamAsMap

挿入
====

挿入には、 ``InsertBuilder`` クラスを使用します。

利用例は次のとおりです。

.. code-block:: java

  InsertBuilder builder = InsertBuilder.newInstance(config);
  builder.sql("insert into Emp");
  builder.sql("(name, salary)");
  builder.sql("values (");
  builder.param(String.class, "SMITH").sql(", ");
  builder.param(BigDecimal.class, new BigDecimal(1000)).sql(")");
  builder.execute();

組み立てたSQLは ``execute`` メソッドで実行できます。

更新
====

更新には、 ``UpdateBuilder`` クラスを使用します。

利用例は次のとおりです。

.. code-block:: java

  UpdateBuilder builder = UpdateBuilder.newInstance(config);
  builder.sql("update Emp");
  builder.sql("set");
  builder.sql("name = ").param(String.class, "SMIHT").sql(",");
  builder.sql("salary = ").param(BigDecimal.class, new BigDecimal("1000"));
  builder.sql("where");
  builder.sql("id = ").param(int.class, 10);
  builder.execute();

組み立てたSQLは ``execute`` メソッドで実行できます。

削除
====

削除には、 ``DeleteBuilder`` クラスを使用します。

利用例は次のとおりです。

.. code-block:: java

  DeleteBuilder builder = DeleteBuilder.newInstance(config);
  builder.sql("delete from Emp");
  builder.sql("where");
  builder.sql("name = ").param(String.class, "SMITH");
  builder.sql("and");
  builder.sql("salary = ").param(BigDecimal.class, new BigDecimal(1000));
  builder.execute();

組み立てたSQLは ``execute`` メソッドで実行できます。

