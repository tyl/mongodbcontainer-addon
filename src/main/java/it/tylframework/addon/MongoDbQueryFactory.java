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

import org.springframework.data.mongodb.repository.MongoRepository;
import org.vaadin.addons.lazyquerycontainer.Query;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;
import org.vaadin.addons.lazyquerycontainer.QueryFactory;

import java.io.Serializable;

/**
 * Created by marco on 12/07/14.
 */
public class MongoDbQueryFactory implements QueryFactory, Serializable {
    /**
     * Java serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor which allows setting the entity manager.
     * @param entityManager the entity manager
     */

    MongoRepository mongoRepository;

    public MongoDbQueryFactory(final MongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }


    @Override
    public Query constructQuery(QueryDefinition queryDefinition) {
        return new MongoDbQuery((MongoDbQueryDefinition) queryDefinition, mongoRepository);
    }
}
