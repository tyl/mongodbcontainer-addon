/**
 * Copyright (c) 2014 - Tyl Consulting s.a.s.
 *
 *    Authors: Edoardo Vacchi
 *    Contributors: Marco Pancotti, Daniele Zonca
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

 package org.tylproject.vaadin.addon;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.filter.UnsupportedFilterException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.tylproject.vaadin.addon.beanfactory.BeanFactory;
import org.tylproject.vaadin.addon.beanfactory.DefaultBeanFactory;
import org.tylproject.vaadin.addon.utils.DefaultFilterConverter;
import org.tylproject.vaadin.addon.utils.FilterConverter;
import org.tylproject.vaadin.addon.utils.Page;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.*;
import java.util.*;
import java.util.logging.Logger;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Simple (non-buffered) Mongo Container
 *
 * Every change to this container will be immediately reflected in the DB.
 * An instance of this container can be obtained through the fluent
 * {@link org.tylproject.vaadin.addon.MongoContainer.Builder}
 *
 * This container is Ordered but not {@link com.vaadin.data.Container.Sortable},
 * because it is not possible to sort it on the fly. You can always
 * build a container for a sorted query using the Builder method
 * {@link org.tylproject.vaadin.addon.MongoContainer.Builder#sortedBy(org.springframework.data.domain.Sort)}.
 *
 * Please notice that it is not possible to {@link #addItem()}
 * or {@link #addItem(Object)}, because it would not be possible to satisfy
 * the contract of the method. The itemId should be a temporary handle,
 * but this container immediately reflects changes onto the DB,
 * so this would result in an empty entity on the DB.
 *
 *
 */
public class MongoContainer<Bean>
        extends AbstractContainer
        implements Container, Container.Ordered, Container.Indexed,
        Container.Filterable, Container.Sortable,
        Container.ItemSetChangeNotifier {



    /**
     * Fluent Builder for a (Buffered)MongoContainer instance.
     *
     * Every Container includes all the properties of the given bean as a default.
     * You can use the methods {@link org.tylproject.vaadin.addon.MongoContainer.Builder#withProperty(java.lang.String, Class)}
     * and {@link org.tylproject.vaadin.addon.MongoContainer.Builder#withNestedProperty(java.lang.String, Class)}
     * to define a custom list of properties.
     *
     * @param <BT> Type of the entity
     */
    public static class Builder<BT> {

        private final static int DEFAULT_PAGE_SIZE = 100;


        private final MongoOperations mongoOps;
        private Criteria mongoCriteria = new Criteria();
        private final Class<BT> beanClass;
        private Sort sort;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private LinkedHashMap<String,Class<?>> simpleProperties = new LinkedHashMap<String, Class<?>>();
        private LinkedHashMap<String,Class<?>> nestedProperties = new LinkedHashMap<String, Class<?>>();

        private boolean hasCustomPropertyList = false;
        private BeanFactory<BT> beanFactory ;
        private FilterConverter filterConverter = new DefaultFilterConverter();

        /**
         * Initializes and return a builder for a MongoContainer
         *
         * @param beanClass class of the entity
         * @param mongoOps mongoOperation instance
         * @param <T> type of the entity
         * @return the builder instance for the given entity,
         *         using the given MongoOperations instance
         */
        public static <T> MongoContainer.Builder<T> forEntity(
                final Class<T> beanClass, final MongoOperations mongoOps) {
            return new MongoContainer.Builder<T>(beanClass, mongoOps);
        }

        private Builder(final Class<BT> beanClass, final MongoOperations mongoOps) {
            this.mongoOps = mongoOps;
            this.beanClass = beanClass;
            this.beanFactory = new DefaultBeanFactory<BT>(beanClass);
        }

        public Builder<BT> withBeanFactory(BeanFactory<BT> beanFactory) {
            this.beanFactory = beanFactory;
            return this;
        }

        /**
         * @param mongoCriteria A {@link org.springframework.data.mongodb.core.query.Criteria}
         *                      object created through Spring's
         *                      fluent interface
         */
        public Builder<BT> forCriteria(final Criteria mongoCriteria) {
            this.mongoCriteria = mongoCriteria;
            return this;
        }


        /**
         * @param sort A Spring {@link org.springframework.data.domain.Sort} object
         */
        public Builder<BT> sortedBy(final Sort sort) {
            this.sort = sort;
            return this;
        }

        /**
         * specify the (internal) page size of the lazy container
         */
        public Builder<BT> withPageSize(final int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * adds a property with the given property id and of the given type
         */
        public Builder<BT> withProperty(String id, Class<?> type) {
            hasCustomPropertyList = true;
            simpleProperties.put(id, type);
            return this;
        }

        /**
         * adds a nested property of the given type.
         *
         * A  <em>nested</em> property for a bean is a property that can be reached
         * through a <i>path</i> like <code>path.to.property</code>.
         *
         * e.g., suppose you have a bean Person, with a property "address"
         * of type Address; suppose that Address has a property "street".
         * In code you can write <code>myPerson.getAddress().getStreet()</code>.
         *
         * You can access such nested properties in the container using the syntax:
         * <pre>
         *     builder.withNestedProperty("address.street");
         * </pre>
         *
         */
        public Builder<BT> withNestedProperty(String id, Class<?> type) {
            hasCustomPropertyList = true;
            nestedProperties.put(id, type);
            return this;
        }

        public Builder<BT> withFilterConverter(FilterConverter customFilterConverter) {
            this.filterConverter = customFilterConverter;
            return this;
        }


        /**
         * @return a simple MongoContainer instance
         */
        public MongoContainer<BT> build() {
            final MongoContainer<BT> mc = new MongoContainer<BT>(this);
            mc.fetchPage(0, pageSize);
            return mc;
        }

        /**
         * @return a BufferedMongoContainer instance
         */
        public BufferedMongoContainer<BT> buildBuffered() {
            final BufferedMongoContainer<BT> mc = new BufferedMongoContainer<BT>(this);
            mc.fetchPage(0, pageSize);
            return mc;
        }
    }

    protected static final String ID = "_id";
    protected static final Logger log = Logger.getLogger("MongoContainer");


    @Nonnull protected Page<ObjectId> page;
    protected final int pageSize;


    protected final Criteria criteria;
    /**
     * criteria updated by {@link #addContainerFilter(com.vaadin.data.Container.Filter)}
     */
    protected Query query;
    protected final Query baseQuery;
    protected final Sort baseSort;
    protected Sort sort;
    protected final FilterConverter filterConverter;
    protected final List<Filter> appliedFilters = new ArrayList<Filter>();
    protected final List<Criteria> appliedCriteria = new ArrayList<Criteria>();

    protected final MongoOperations mongoOps;

    protected final Class<Bean> beanClass;
    protected final BeanFactory<Bean> beanFactory;

    protected final Map<String, Class<?>> simpleProperties ;
    protected final Map<String, Class<?>> nestedProperties ;

    protected final List<Object> allProperties;

    MongoContainer(Builder<Bean> bldr) {
        this.criteria = bldr.mongoCriteria;
        this.baseSort = bldr.sort;
        this.filterConverter = bldr.filterConverter;
        this.baseQuery = Query.query(criteria).with(baseSort);
        resetQuery();



        this.mongoOps = bldr.mongoOps;

        this.beanClass = bldr.beanClass;
        this.beanFactory = bldr.beanFactory;

        if (bldr.hasCustomPropertyList) {
            this.simpleProperties = Collections.unmodifiableMap(bldr
                    .simpleProperties);
            this.nestedProperties = Collections.unmodifiableMap(bldr.nestedProperties);

        } else {

            // otherwise, default to non-nested
            this.simpleProperties = new LinkedHashMap<String, Class<?>>();
            PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(beanClass);
            for (PropertyDescriptor d: descriptors) {
                this.simpleProperties.put(d.getName(), d.getPropertyType());
            }
            nestedProperties = Collections.emptyMap();
        }


        List<Object> allProps = new ArrayList<Object>(simpleProperties.keySet());
        this.allProperties = Collections.unmodifiableList(allProps);
        allProps.addAll(nestedProperties.keySet());

        this.pageSize = bldr.pageSize;

    }

    /**
     * @return a cursor for the query object of this Container instance
     */
    protected DBCursor cursor() {
        DBObject criteriaObject = query.getQueryObject();

        DBObject projectionObject = new BasicDBObject(ID, true);

        String collectionName = mongoOps.getCollectionName(beanClass);
        DBCollection dbCollection = mongoOps.getCollection(collectionName);

        // TODO: keep cursor around to possibly reuse
        DBCursor cursor = dbCollection.find(criteriaObject, projectionObject);

        if (this.baseSort != null || this.sort != null) {
            DBObject sortObject = this.query.getSortObject();
            cursor.sort(sortObject);
        }

        return cursor;
    }

    /**
     * returns a cursor in the given range.
     *
     * shorthand for <pre>
     *      cursor().skip(skip).limit(limit);
     * </pre>
     */
    protected DBCursor cursorInRange(int skip, int limit) {
        return cursor().skip(skip).limit(limit);
    }

    /**
     * fetches a {@link org.tylproject.vaadin.addon.utils.Page}
     * within the given range
     *
     */
    protected void fetchPage(int offset, int pageSize) {

        // TODO: keep cursor around to possibly reuse
        DBCursor cursor = cursorInRange(offset, pageSize);

        Page<ObjectId> newPage = new Page<ObjectId>(pageSize, offset, this.size());

        for (int i = offset; cursor.hasNext(); i++)
            newPage.set(i, (ObjectId) cursor.next().get(ID));

        this.page = newPage;
    }

    /**
     * returns the current page and refreshes it when invalid
     */
    protected Page<ObjectId> page() {
        if (!page.isValid()) fetchPage(page.offset, page.pageSize);
        return page;
    }

    public BeanItem<Bean> getItem(Object o) {
        assertIdValid(o);
        final Bean document = mongoOps.findById(o, beanClass);
        // document was not found in the actual DB
        // but it was in the ID cache
        // then the cache is invalid
        if (document == null && page.contains(o)) {
            refresh();
        }
        return makeBeanItem(document);
    }

    protected BeanItem<Bean> makeBeanItem(Bean document) {
        if (document == null) return null;

        final BeanItem<Bean> beanItem = new BeanItem<Bean>(document, this.simpleProperties.keySet());
        for (String nestedPropId: nestedProperties.keySet()) {
            beanItem.addNestedProperty(nestedPropId);
        }
        return beanItem;
    }

    @Override
    public Collection<?> getContainerPropertyIds() {
        return this.allProperties;
    }

    // method is basically deprecated
    @Override
    @Deprecated
    public List<ObjectId> getItemIds() {
        log.info("this expensive operation should be avoided");
        return getItemIds(0, this.size());
    }

    @Override
    public Property<?> getContainerProperty(Object itemId, Object propertyId) {
        BeanItem<Bean> item = getItem(itemId);
        if (item == null) return null;

        return item.getItemProperty(propertyId);
    }

    // return the data type of the given property id
    @Override
    public Class<?> getType(Object propertyId) {
        if (simpleProperties.containsKey(propertyId)) return simpleProperties.get(propertyId);
        else if (nestedProperties.containsKey(propertyId)) return nestedProperties.get(propertyId);

        throw new IllegalArgumentException("Cannot find the given propertyId: " + propertyId);
    }

    @Override
    public int size() {
        return (int) mongoOps.count(query, beanClass);
    }

    @Override
    public boolean containsId(Object itemId) {
        assertIdValid(itemId);
        Query q = makeBaseQuery().addCriteria(where(ID).is(itemId));
        return mongoOps.exists(q, beanClass);
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public BeanItem<Bean> addItem(Object itemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "cannot addItem(); insert() into mongo or build a buffered container");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public ObjectId addItem() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "cannot addItem(); insert() into mongo or build a buffered container");
    }

    /**
     * performs an upsert of the given target bean
     */
    public ObjectId addEntity(Bean target) {
        mongoOps.save(target);
        refresh();
        return this.beanFactory.getId(target);
    }

    @Override
    public boolean removeItem(Object itemId) throws UnsupportedOperationException {
        Query q = makeBaseQuery().addCriteria(where(ID).is(itemId));
        mongoOps.findAndRemove(q, beanClass);
        refresh();
        fireItemSetChange();
        return true;
    }

    @Override
    public boolean addContainerProperty(Object o, Class<?> aClass, Object o2) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("cannot add container property dynamically; use Builder");
    }

    @Override
    public boolean removeContainerProperty(Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAllItems() throws UnsupportedOperationException {
        mongoOps.remove(this.query, beanClass);
        refresh();
        fireItemSetChange();
        return true;
    }

    @Override
    public int indexOfId(Object itemId) {
        ObjectId oid = assertIdValid(itemId);

        // for the principle of locality,
        // let us optimistically first check within the page
        int index = page().indexOf(oid);
        if (index > -1) return index;

        // otherwise, linearly scan the entire collection using a cursor
        // and only fetch the ids
        DBCursor cur = cursor();
        for (int i = 0; cur.hasNext(); i++) {
            // skip the check for those already in the page
            DBObject value = cur.next();
            if (i >= page.offset && i < page.maxIndex) {
                continue;
            }
            if (value.get(ID).equals(itemId)) return i;
        }
        return -1;
    }

    @Override
    @Nullable
    public ObjectId getIdByIndex(int index) {
        if (index < 0 || size() == 0) return null;
        DBCursor cur = cursorInRange(index, 1);
        return cur.hasNext()?
                (ObjectId)cur.next().get(ID)
                : null;
    }

    @Override
    public List<ObjectId> getItemIds(int startIndex, int numberOfItems) {
        //List<BeanId> beans = mongoOps.find(Query.query(criteria).skip(startIndex).limit(numberOfItems), BeanId.class);
        //List<ObjectId> ids = new PropertyList<ObjectId,BeanId>(beans, beanIdDescriptor, "_id");
        log.info(String.format("range: [%d,%d]", startIndex, numberOfItems));
        if (page.isValid() && page.isWithinRange(startIndex, numberOfItems)) {
            return page.subList(startIndex, numberOfItems); // return the requested range
        }

        fetchPage(startIndex, numberOfItems);

        return this.page.toImmutableList();
    }

    @Override
    public Object addItemAt(int index) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item addItemAt(int index, Object newItemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId nextItemId(Object itemId) {
        int index = indexOfId(itemId);
        return getIdByIndex(index+1);
    }

    @Override
    public ObjectId prevItemId(Object itemId) {
        int index = indexOfId(itemId);
        return getIdByIndex(index - 1);
    }

    @Override
    public ObjectId firstItemId() {
        return getIdByIndex(0);
    }

    @Override
    public ObjectId lastItemId() {
        return size() > 0 ?
                 getIdByIndex(size()-1)
               : null;
    }
    @Override
    public boolean isFirstId(Object itemId) {
        assertIdValid(itemId);
        return itemId.equals(firstItemId());
    }

    @Override
    public boolean isLastId(Object itemId) {
        assertIdValid(itemId);
        return itemId.equals(lastItemId());
    }

    @Override
    public Object addItemAfter(Object previousItemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item addItemAfter(Object previousItemId, Object newItemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addItemSetChangeListener(ItemSetChangeListener listener) {
        super.addItemSetChangeListener(listener);
    }

    @Override
    public void addListener(ItemSetChangeListener listener) {
        super.addListener(listener);
    }

    @Override
    public void removeItemSetChangeListener(ItemSetChangeListener listener) {
        super.removeItemSetChangeListener(listener);
    }

    @Override
    public void removeListener(ItemSetChangeListener listener) {
        super.removeListener(listener);
    }

    @Override
    protected void fireItemSetChange() {
        page.setInvalid();
        super.fireItemSetChange();
    }

    /**
     * invalidate the internal page and reload it
     */
    public void refresh() {
        page.setInvalid();
        page();
    }

    protected Query makeBaseQuery() {
        return Query.query(criteria).with(baseSort);
    }

    /**
     * Verifies that the given Object instance is a valid itemId
     *
     * Note: This should generally be done through type-checking of the parameter
     * but this is not possible because of Vaadin's interfaces
     *
     * @throws java.lang.NullPointerException if the parameter is null
     * @throws java.lang.IllegalArgumentException if the parameter is not an ObjectId
     */
    protected ObjectId assertIdValid(Object o) {
        if ( o == null )
            throw new NullPointerException("Id cannot be null");
        if ( ! ( o instanceof ObjectId ) )
            throw new IllegalArgumentException("Id is not instance of ObjectId: "+o);

        return (ObjectId) o;
    }

    @Override
    public void addContainerFilter(Filter filter) throws UnsupportedFilterException {
        Criteria c = filterConverter.convert(filter);
        this.query.addCriteria(c);
        appliedCriteria.add(c);
        appliedFilters.add(filter);
        page.setInvalid();
    }

    @Override
    public void removeContainerFilter(Filter filter) {
        appliedFilters.remove(filter);
        removeAllContainerFilters();
        for (Filter f: appliedFilters) {
            addContainerFilter(f);
        }
    }

    @Override
    public void removeAllContainerFilters() {
        resetQuery();
        applySort(this.query, this.sort);
        page.setInvalid();
    }

    protected void resetQuery() {
        this.query = makeBaseQuery();
        this.appliedFilters.clear();
        this.appliedCriteria.clear();
        this.sort = null;
    }



    @Override
    public Collection<Filter> getContainerFilters() {
        return Collections.unmodifiableList(new ArrayList<Filter>(appliedFilters));
    }


    @Override
    public void sort(Object[] propertyId, boolean[] ascending) {
        if (propertyId.length != ascending.length)
            throw new IllegalArgumentException(
                    String.format(
                    "propertyId array length does not match" +
                            "ascending array length (%d!=%d)",
                            propertyId.length,
                            ascending.length));

        Sort result = null;

        // if the arrays are empty, will just the conditions

        if (propertyId.length != 0) {
            result = new Sort(
                    ascending[0] ? Sort.Direction.ASC : Sort.Direction.DESC,
                    propertyId[0].toString());
            for (int i = 1; i < propertyId.length; i++) {
                result = result.and(new Sort(
                        ascending[i] ? Sort.Direction.ASC : Sort.Direction.DESC,
                        propertyId[i].toString()));
            }
        }

        resetQuery();

        applySort(this.query, result);
        applyCriteriaList(this.query, appliedCriteria);

        this.sort = result;

        refresh();

    }

    protected Query applySort(Query q, Sort s) {
        q.with(s);
        return q;
    }
    protected Query applyCriteriaList(Query q, List<Criteria> criteriaList) {
        for (Criteria c: criteriaList)
            q.addCriteria(c);
        return q;
    }

    @Override
    public Collection<?> getSortableContainerPropertyIds() {
        return getContainerPropertyIds();
    }

}
