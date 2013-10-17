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
package org.seasar.doma.internal.jdbc.command;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import java.util.function.Supplier;

import org.seasar.doma.internal.wrapper.Scalar;
import org.seasar.doma.jdbc.query.SelectQuery;

/**
 * @author taedium
 * 
 */
public class ScalarSingleResultHandler<BASIC, CONTAINER> extends
        AbstractSingleResultHandler<CONTAINER> {

    protected final Supplier<Scalar<BASIC, CONTAINER>> supplier;

    public ScalarSingleResultHandler(Supplier<Scalar<BASIC, CONTAINER>> supplier) {
        assertNotNull(supplier);
        this.supplier = supplier;
    }

    @Override
    protected ScalarResultProvider<BASIC, CONTAINER> createResultProvider(
            SelectQuery query) {
        return new ScalarResultProvider<>(supplier, query);
    }
}
