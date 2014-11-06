package it.tylframework.vaadin.addon;

import com.mongodb.DBObject;
import com.sun.javaws.jnl.PropertyDesc;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

import com.vaadin.data.util.AbstractContainer;
import com.vaadin.data.util.BeanItem;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;


import java.beans.*;
import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by evacchi on 26/09/14.
 */
public class MongoContainer<Bean,Id>
        //extends AbstractContainer
        implements Container, Container.Ordered, Container.Indexed {


    public static class Builder {
        private final MongoOperations mongoOps;
        private Criteria mongoCriteria;
        private Class<?> beanClass;
        private Class<?> idClass;
        private Map<Object,Class<?>> ids = new HashMap<Object, Class<?>>();

        public static MongoContainer.Builder with(final MongoOperations mongoOps) {
            return new MongoContainer.Builder(mongoOps);
        }

        private Builder(final MongoOperations mongoOps) {
            this.mongoOps = mongoOps;
        }

        public Builder forCriteria(final Criteria mongoCriteria) {
            this.mongoCriteria = mongoCriteria;
            return this;
        }

        public Builder withBeanClass(final Class<?> beanClass) {
            this.beanClass = beanClass;
            return this;
        }

        public Builder withIdClass(final Class<?> idClass) {
            this.idClass = idClass;
            return this;
        }

        public Builder withProperty(Object id, Class<?> type) {
            ids.put(id, type);
            return this;
        }

        public <Bean,Id> MongoContainer<Bean,Id> build() {
            MongoContainer<Bean,Id> mc = new MongoContainer<Bean,Id>(mongoCriteria, mongoOps, (Class<Bean>) beanClass, (Class<Id>) idClass);
            for (Object id: ids.keySet()) {
                mc.addContainerProperty(id, ids.get(id), null);
            }
            return mc;
        }

    }

    class BeanId {
        @org.springframework.data.annotation.Id
        private Object _id;
    }


    /*
    public static <Bean,Id> MongoContainer<Bean,Id> forQuery(Query mongoQuery, Class<Bean> beanClass, Class<Id> idClass) {
        return new MongoContainer(mongoQuery, beanClass, idClass);
    }
    */

    private static final String ID = "_id";

    private final Criteria criteria;
    private final Query query;
    private final MongoOperations mongoOps;

    private final Class<Bean> beanClass;
    private final Class<Id> idClass;
    private final BeanDescriptor beanDescriptor;

    private final LinkedHashMap<String, PropertyDescriptor> propertyDescriptorMap;

    private MongoContainer(final Criteria criteria,
                           final MongoOperations mongoOps,
                           final Class<Bean> beanClass,
                           final Class<Id> idClass) {

        this.criteria = criteria;
        this.query = Query.query(criteria);
        this.mongoOps = mongoOps;

        this.beanClass = beanClass;
        this.idClass = idClass;

        this.beanDescriptor = getBeanDescriptor(beanClass);
        this.propertyDescriptorMap = getBeanPropertyDescriptor(beanClass);
    }

    public BeanItem getItem(Object o) {
        assertIdValid(o);
        final Bean document = mongoOps.findById(o, beanClass);
        return new BeanItem<Bean>(document);
    }

    @Override
    public Collection<?> getContainerPropertyIds() {
        try {
            return getVaadinPropertyIds(beanClass);
        } catch (Exception e) { throw new Error(e); }
    }

    @Override
    public List<Id> getItemIds() {
        throw new UnsupportedOperationException("this expensive operation is unsupported");
//        Query q = Query.query(criteria).fields().include(ID);
//        List<BeanId> beans = mongoOps.find(q, beanClass);
//        return new PropertyList<Id,Bean>(beans, beanDescriptor, "id");
    }

    @Override
    public Property getContainerProperty(Object itemId, Object propertyId) {
        return getItem(itemId).getItemProperty(propertyId);
    }

    // return the data type of the given property id
    @Override
    public Class<?> getType(Object propertyId) {
        PropertyDescriptor pd = propertyDescriptorMap.get(propertyId);
        return pd == null? null: pd.getPropertyType();
    }

    public int size() {
        return (int) mongoOps.count(query, beanClass);
    }

    @Override
    public boolean containsId(Object itemId) {
        return mongoOps.exists(Query.query(where(ID).is(itemId)), beanClass);
    }

    @Override
    public Item addItem(Object o) throws UnsupportedOperationException {
        return null;
    }

    @Override
    public Object addItem() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public boolean removeItem(Object o) throws UnsupportedOperationException {
        return false;
    }

    @Override
    public boolean addContainerProperty(Object o, Class<?> aClass, Object o2) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeContainerProperty(Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAllItems() throws UnsupportedOperationException {
        return false;
    }

    @Override
    public int indexOfId(Object itemId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Id getIdByIndex(int index) {
        return getItemIds().get(index);
    }

    @Override
    public List<Id> getItemIds(int startIndex, int numberOfItems) {
        List<Bean> beans = mongoOps.find(Query.query(criteria).skip(startIndex).limit(numberOfItems), beanClass);
        List<Id> ids = new PropertyList<Id,Bean>(beans, beanDescriptor, "id");
        return ids;
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
    public Id nextItemId(Object itemId) {
        List<Id> itemIds = getItemIds();
        return itemIds.get(itemIds.indexOf(itemId) + 1);
    }

    @Override
    public Id prevItemId(Object itemId) {
        List<Id> itemIds = getItemIds();
        return itemIds.get(itemIds.indexOf(itemId)-1);
    }

    @Override
    public Id firstItemId() {
        List<Id> itemIds = getItemIds();
        return itemIds.get(itemIds.indexOf(0));
    }

    @Override
    public Id lastItemId() {
        List<Id> itemIds = getItemIds();
        return itemIds.get(itemIds.indexOf(itemIds.size()-1));
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


    private void assertIdValid(Object o) {
        if ( o == null )
            throw new NullPointerException("Id cannot be null");
        if ( ! idClass.isInstance(o) )
            throw new IllegalArgumentException("Id is not of the given type: "+o.getClass());
    }

    private static <T> BeanDescriptor getBeanDescriptor(Class<T> beanClass) {
        try {
            return Introspector.getBeanInfo(beanClass).getBeanDescriptor();
        } catch (Exception ex) { throw new Error(ex); }
    }

    private static LinkedHashMap<String, PropertyDescriptor> getBeanPropertyDescriptor(
            final Class<?> beanClass) {

        try {

            LinkedHashMap<String, PropertyDescriptor> propertyDescriptorMap =
                new LinkedHashMap<String, PropertyDescriptor>();

            if (beanClass.isInterface()) {

                for (Class<?> cls : beanClass.getInterfaces()) {
                    propertyDescriptorMap.putAll(getBeanPropertyDescriptor(cls));
                }

                BeanInfo info = Introspector.getBeanInfo(beanClass);
                for (PropertyDescriptor pd: info.getPropertyDescriptors()) {
                    propertyDescriptorMap.put(pd.getName(), pd);
                }

            } else {
                BeanInfo info = Introspector.getBeanInfo(beanClass);
                for (PropertyDescriptor pd: info.getPropertyDescriptors()) {
                    propertyDescriptorMap.put(pd.getName(), pd);
                }
            }

            return propertyDescriptorMap;
        } catch (Exception ex) { throw new Error(ex); }

    }

    private static Set<?> getVaadinPropertyIds(final Class<?> beanClass) throws IntrospectionException {
        LinkedHashMap<String,PropertyDescriptor> propDescrs = getBeanPropertyDescriptor(beanClass);


        return Collections.unmodifiableSet(propDescrs.entrySet());
    }

}
