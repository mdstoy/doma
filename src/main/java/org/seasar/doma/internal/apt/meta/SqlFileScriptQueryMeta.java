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
package org.seasar.doma.internal.apt.meta;

import javax.lang.model.element.ExecutableElement;

import org.seasar.doma.internal.apt.mirror.ScriptMirror;
import org.seasar.doma.jdbc.SqlLogType;

/**
 * @author taedium
 * 
 */
public class SqlFileScriptQueryMeta extends AbstractSqlFileQueryMeta {

    protected ScriptMirror scriptMirror;

    public SqlFileScriptQueryMeta(ExecutableElement method) {
        super(method);
    }

    void setScriptMirror(ScriptMirror scriptMirror) {
        this.scriptMirror = scriptMirror;
    }

    ScriptMirror getScriptMirror() {
        return scriptMirror;
    }

    public boolean getHaltOnError() {
        return scriptMirror.getHaltOnErrorValue();
    }

    public String getBlockDelimiter() {
        return scriptMirror.getBlockDelimiterValue();
    }

    public SqlLogType getSqlLogType() {
        return scriptMirror.getSqlLogValue();
    }

    @Override
    public <R, P> R accept(QueryMetaVisitor<R, P> visitor, P p) {
        return visitor.visitSqlFileScriptQueryMeta(this, p);
    }

}
