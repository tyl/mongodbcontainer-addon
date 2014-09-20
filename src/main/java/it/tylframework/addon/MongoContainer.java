/*
 * Copyright (c) 2014 - Marco Pancotti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.tylframework.addon;

import org.springframework.data.mongodb.core.MongoOperations;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;
import org.vaadin.addons.lazyquerycontainer.QueryFactory;
import org.vaadin.addons.lazyquerycontainer.QueryView;

/**
 * Created by marco on 12/07/14.
 */
public class MongoContainer<E> extends LazyQueryContainer {

    public static final int DefaultBatchSize = 100;

    public MongoContainer(final Class<E> beanClass,
                          final Object idPropertyId,
                          final MongoOperations mongoOps) {
        super(new MongoQueryFactory(mongoOps, beanClass), idPropertyId, DefaultBatchSize, false);
    }


    public MongoContainer(MongoQueryFactory queryFactory, Object idPropertyId, int batchSize, boolean compositeItems) {
        super(queryFactory, idPropertyId, batchSize, compositeItems);
    }


    public MongoContainer(QueryDefinition queryDefinition, MongoQueryFactory queryFactory) {
        super(queryDefinition, queryFactory);
    }

    public MongoContainer(QueryView queryView) {
        super(queryView);
    }
}
