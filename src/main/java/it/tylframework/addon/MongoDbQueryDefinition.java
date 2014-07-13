/**
 * Copyright 2014 Marco Pancotti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.tylframework.addon;

import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;

/**
 * Created by marco on 12/07/14.
 */
public class MongoDbQueryDefinition extends LazyQueryDefinition {
    /**
     * Serial version UID for this class.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Class of the mongodb entity type.
     */
    private Class<?> mongoClass;

    /**
     * Constructor for configuring query definition.
     *
     * @param compositeItems                 True if items are wrapped to CompositeItems.
     * @param mongoClass                     The mongodb class.
     * @param batchSize                      The batch size.
     * @param idPropertyId                   The ID of the ID property or null if item index is used as ID.
     */
    public MongoDbQueryDefinition(final boolean compositeItems,
                                 final Class<?> mongoClass,
                                 final int batchSize,
                                 final Object idPropertyId) {
        super(compositeItems, batchSize, idPropertyId);
        this.mongoClass = mongoClass;
    }

    public final Class<?> getMongoClass() {
        return mongoClass;
    }
}
