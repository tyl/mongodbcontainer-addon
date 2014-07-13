/*
 * Copyright 2014 Marco Pancotti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.tylframework.addon;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.vaadin.addons.lazyquerycontainer.Query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by marco on 12/07/14.
 * Implementation of org.vaadin.addons.lazyquerycontainer.Query for
 * MongoDb database
 */
public class MongoDbQuery<E> implements Query, Serializable {
    /**
     * Java serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbQuery.class);

    /**
     * The MongoDb entity class.
     */
    private final Class<E> mongoClass;


    /** The Repository */
    private final MongoRepository repository;

    /** The MongoDbQueryDefinition */
    private final MongoDbQueryDefinition mongoDbQueryDefinition;

    public MongoDbQuery(
            final MongoDbQueryDefinition mongoDbQueryDefinition,
            final MongoRepository repository) {
        this.repository=repository;
        this.mongoDbQueryDefinition=mongoDbQueryDefinition;
        this.mongoClass = (Class<E>) mongoDbQueryDefinition.getMongoClass();
    }


    @Override
    public int size() {
        // TODO - applicare i filtri prima di calcolare il size
        return (int) repository.count();
    }

    /**
     *
     * @param startIndex
     *      first page starting from 0
     * @param count
     *      number of items per page
     * @return
     *      list of Items filled of BeanItems
     */
    @Override
    public List<Item> loadItems(int startIndex, int count) {
        List<Item> items = new ArrayList<Item>();
        Page<E> page = repository.findAll(new PageRequest(startIndex,count));
        for(E element:page.getContent()){
            items.add(new BeanItem<E>(element));
        }
        return items;
    }

    @Override
    public void saveItems(List<Item> addedItems, List<Item> modifiedItems, List<Item> removedItems) {

    }

    @Override
    public boolean deleteAllItems() {
        return false;
    }

    @Override
    public Item constructItem() {
        return null;
    }
}
