package org.seasar.doma.jdbc.query;

import java.net.URL;
import org.seasar.doma.jdbc.SqlLogType;

public interface ScriptQuery extends Query {

  URL getScriptFileUrl();

  String getScriptFilePath();

  String getBlockDelimiter();

  boolean getHaltOnError();

  SqlLogType getSqlLogType();
}
