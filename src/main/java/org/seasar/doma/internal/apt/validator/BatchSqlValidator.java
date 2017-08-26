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
package org.seasar.doma.internal.apt.validator;

import java.util.LinkedHashMap;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import org.seasar.doma.internal.apt.Context;
import org.seasar.doma.internal.apt.reflection.SuppressReflection;
import org.seasar.doma.internal.jdbc.sql.node.EmbeddedVariableNode;
import org.seasar.doma.internal.jdbc.sql.node.ForNode;
import org.seasar.doma.internal.jdbc.sql.node.IfNode;
import org.seasar.doma.message.Message;

/**
 * @author taedium
 * 
 */
public class BatchSqlValidator extends SqlValidator {

    private boolean embeddedVariableWarningNotified;

    private boolean ifWarningNotified;

    private boolean forWarningNotified;

    private SuppressReflection suppressReflection;

    public BatchSqlValidator(Context ctx, ExecutableElement methodElement,
            LinkedHashMap<String, TypeMirror> parameterTypeMap, String path, boolean expandable,
            boolean populatable) {
        super(ctx, methodElement, parameterTypeMap, path, expandable, populatable);
        suppressReflection = ctx.getReflections().newSuppressReflection(methodElement);
    }

    @Override
    public Void visitEmbeddedVariableNode(EmbeddedVariableNode node, Void p) {
        if (!isSuppressed(Message.DOMA4181) && !embeddedVariableWarningNotified) {
            ctx.getNotifier().send(Kind.WARNING, Message.DOMA4181, methodElement,
                    new Object[] { path });
            embeddedVariableWarningNotified = true;
        }
        return super.visitEmbeddedVariableNode(node, p);
    }

    @Override
    public Void visitIfNode(IfNode node, Void p) {
        if (!isSuppressed(Message.DOMA4182) && !ifWarningNotified) {
            ctx.getNotifier().send(Kind.WARNING, Message.DOMA4182, methodElement,
                    new Object[] { path });
            ifWarningNotified = true;
        }
        return super.visitIfNode(node, p);
    }

    @Override
    public Void visitForNode(ForNode node, Void p) {
        if (!isSuppressed(Message.DOMA4183) && !forWarningNotified) {
            ctx.getNotifier().send(Kind.WARNING, Message.DOMA4183, methodElement,
                    new Object[] { path });
            forWarningNotified = true;
        }
        return super.visitForNode(node, p);
    }

    private boolean isSuppressed(Message message) {
        if (suppressReflection != null) {
            return suppressReflection.isSuppressed(message);
        }
        return false;
    }

}