package it.tylframework.vaadin.addon;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

import com.vaadin.data.util.BeanItem;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.beans.*;
import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by evacchi on 26/09/14.
 */
public class MongoContainer<Bean,Id> implements Container, Container.Ordered, Container.Indexed {



    public static class Builder {
        private final MongoOperations mongoOps;
        private Query mongoQuery;
        private Class<?> beanClass;
        private Class<?> idClass;
        private Map<Object,Class<?>> ids = new HashMap<Object, Class<?>>();

        public static MongoContainer.Builder with(final MongoOperations mongoOps) {
            return new MongoContainer.Builder(mongoOps);
        }

        private Builder(final MongoOperations mongoOps) {
            this.mongoOps = mongoOps;
        }

        public Builder forQuery(final Query mongoQuery) {
            this.mongoQuery = mongoQuery;
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
            MongoContainer<Bean,Id> mc = new MongoContainer<Bean,Id>(mongoQuery, mongoOps, (Class<Bean>) beanClass, (Class<Id>) idClass);
            for (Object id: ids.keySet()) {
                mc.addContainerProperty(id, ids.get(id), null);
            }
            return mc;
        }

    }


    /*
    public static <Bean,Id> MongoContainer<Bean,Id> forQuery(Query mongoQuery, Class<Bean> beanClass, Class<Id> idClass) {
        return new MongoContainer(mongoQuery, beanClass, idClass);
    }
    */


    private final Query query;
    private final MongoOperations mongoOps;

    private final Class<Bean> beanClass;
    private final Class<Id> idClass;
    private final BeanDescriptor beanDescriptor;


    private MongoContainer(final Query query,
                           final MongoOperations mongoOps,
                           final Class<Bean> beanClass,
                           final Class<Id> idClass) {

        this.query     = query;
        this.mongoOps  = mongoOps;

        this.beanClass = beanClass;
        this.idClass   = idClass;

        this.beanDescriptor = getBeanDescriptor(beanClass);

    }

    public Query getQuery() { return query; }

    @Override
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
        query.fields().include("_id");
        List<Bean> beans = mongoOps.find(query, beanClass);
        return new PropertyList<Id,Bean>(beans, beanDescriptor, "id");
    }

    @Override
    public Property getContainerProperty(Object o, Object o2) {
        return null;
    }

    // return the data type of the given property id
    @Override
    public Class<?> getType(Object o) {
        return null;
    }

    public int size() {
        return (int) mongoOps.count(query, beanClass);
    }

    @Override
    public boolean containsId(Object o) {
        return mongoOps.exists(Query.query(where("_id").is(o)), beanClass);
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
        assertIdValid(itemId);
        List<Bean> beans = mongoOps.find(query, beanClass);
        List<Id> ids = new PropertyList<Id,Bean>(beans, beanDescriptor, "id");
        return ids.indexOf(itemId);
    }

    @Override
    public Id getIdByIndex(int index) {
        return getItemIds().get(index);
    }

    @Override
    public List<Id> getItemIds(int startIndex, int numberOfItems) {
        List<Bean> beans = mongoOps.find(query.skip(startIndex).limit(numberOfItems), beanClass);
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
        return itemIds.get(itemIds.indexOf(itemId)+1);
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

    private static List<PropertyDescriptor> getBeanPropertyDescriptor(
            final Class<?> beanClass) throws IntrospectionException {
        // Oracle bug 4275879: Introspector does not consider superinterfaces of
        // an interface
        if (beanClass.isInterface()) {
            List<PropertyDescriptor> propertyDescriptors = new ArrayList<PropertyDescriptor>();

            for (Class<?> cls : beanClass.getInterfaces()) {
                propertyDescriptors.addAll(getBeanPropertyDescriptor(cls));
            }

            BeanInfo info = Introspector.getBeanInfo(beanClass);
            propertyDescriptors.addAll(Arrays.asList(info
                    .getPropertyDescriptors()));

            return propertyDescriptors;
        } else {
            BeanInfo info = Introspector.getBeanInfo(beanClass);
            return Arrays.asList(info.getPropertyDescriptors());
        }
    }

    private static List<Object> getVaadinPropertyIds(final Class<?> beanClass) throws IntrospectionException {
        List<PropertyDescriptor> propDescrs = getBeanPropertyDescriptor(beanClass);
        List<Object> propertyIds = new ArrayList<Object>();

        for (PropertyDescriptor p: propDescrs) {
            propertyIds.add(p.getName());
        }

        return Collections.unmodifiableList(propertyIds);
    }

}
