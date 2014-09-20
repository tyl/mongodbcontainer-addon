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
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by marco on 12/07/14.
 * Implementation of org.vaadin.addons.lazyquerycontainer.Query for
 * MongoDb database
 */
public class MongoQuery<E> implements org.vaadin.addons.lazyquerycontainer.Query, Serializable {
    /**
     * Java serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoQuery.class);

    /**
     * The MongoDb entity class.
     */
    private final Class<E> beanClass;


    /** The Repository */
    private final MongoOperations mongoOps;
    private Query currentQuery;

    public MongoQuery(
            final MongoOperations mongoOps,
            final Query currentQuery,
            final Class<E> beanClass) {
        this.mongoOps = mongoOps;
        this.currentQuery = currentQuery;
        this.beanClass = beanClass;
    }


    @Override
    public int size() {
        return (int) mongoOps.count(currentQuery, beanClass);
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
        List<E> page = mongoOps.find(
                currentQuery.skip(startIndex).limit(count),
                beanClass
        );
        for(E element: page){
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
