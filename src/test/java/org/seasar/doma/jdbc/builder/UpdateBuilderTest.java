package org.seasar.doma.jdbc.builder;

import java.math.BigDecimal;
import junit.framework.TestCase;
import org.seasar.doma.internal.jdbc.mock.MockConfig;

public class UpdateBuilderTest extends TestCase {

  public void test() throws Exception {
    UpdateBuilder builder = UpdateBuilder.newInstance(new MockConfig());
    builder.sql("update Emp");
    builder.sql("set");
    builder.sql("name = ").param(String.class, "SMIHT").sql(",");
    builder.sql("salary = ").param(BigDecimal.class, new BigDecimal("1000"));
    builder.sql("where");
    builder.sql("ID = ").param(int.class, 10);
    builder.execute();
  }

  public void testGetSql() throws Exception {
    UpdateBuilder builder = UpdateBuilder.newInstance(new MockConfig());
    builder.sql("update Emp");
    builder.sql("set");
    builder.sql("name = ").param(String.class, "SMIHT").sql(",");
    builder.sql("salary = ").param(BigDecimal.class, new BigDecimal("1000"));
    builder.sql("where");
    builder.sql("ID = ").param(int.class, 10);

    String sql =
        String.format(
            "update Emp%n" + "set%n" + "name = ?,%n" + "salary = ?%n" + "where%n" + "ID = ?");
    assertEquals(sql, builder.getSql().getRawSql());

    builder.execute();
  }

  public void testLiteral() throws Exception {
    UpdateBuilder builder = UpdateBuilder.newInstance(new MockConfig());
    builder.sql("update Emp");
    builder.sql("set");
    builder.sql("name = ").literal(String.class, "SMITH").sql(",");
    builder.sql("salary = ").literal(BigDecimal.class, new BigDecimal("1000"));
    builder.sql("where");
    builder.sql("ID = ").param(int.class, 10);

    String sql =
        String.format(
            "update Emp%n"
                + "set%n"
                + "name = 'SMITH',%n"
                + "salary = 1000%n"
                + "where%n"
                + "ID = ?");
    assertEquals(sql, builder.getSql().getRawSql());

    builder.execute();
  }
}
