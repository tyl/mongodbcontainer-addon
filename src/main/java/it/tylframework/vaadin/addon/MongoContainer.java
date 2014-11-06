package it.tylframework.vaadin.addon;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.beans.*;
import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by evacchi on 26/09/14.
 */
public class MongoContainer<Id, Bean>
        //extends AbstractContainer
        implements Container, Container.Ordered, Container.Indexed {


    public static class Builder {
        private final MongoOperations mongoOps;
        private Criteria mongoCriteria;
        private Class<?> beanClass;
        private Class<?> idClass = ObjectId.class;
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

        public Builder withIdClassClass(final Class<?> idClass) {
            this.beanClass = idClass;
            return this;
        }

        public Builder withProperty(Object id, Class<?> type) {
            ids.put(id, type);
            return this;
        }

        public <Id,Bean> MongoContainer<Id,Bean> build() {
            MongoContainer<Id,Bean> mc = new MongoContainer<Id,Bean>(mongoCriteria, mongoOps, (Class<Id>) idClass, (Class<Bean>) beanClass);
            for (Object id: ids.keySet()) {
                mc.addContainerProperty(id, ids.get(id), null);
            }
            return mc;
        }

    }

    private static final String ID = "_id";

    private final Criteria criteria;
    private final Query query;
    private final MongoOperations mongoOps;

    private final Class<Id> idClass;
    private final Class<Bean> beanClass;
    private final BeanDescriptor beanDescriptor;

    private final LinkedHashMap<String, PropertyDescriptor> propertyDescriptorMap;

    private MongoContainer(final Criteria criteria,
                           final MongoOperations mongoOps,
                           final Class<Id> idClass,
                           final Class<Bean> beanClass) {
        this.criteria = criteria;
        this.query = Query.query(criteria);
        this.mongoOps = mongoOps;

        this.idClass = idClass;
        this.beanClass = beanClass;

        this.beanDescriptor = getBeanDescriptor(beanClass);
        this.propertyDescriptorMap = getBeanPropertyDescriptor(beanClass);
    }

    public BeanItem<Bean> getItem(Object o) {
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
    @Deprecated
    public List<Id> getItemIds() {
        throw new UnsupportedOperationException("this expensive operation is unsupported");
//        Query q = Query.query(criteria).fields().include(ID);
//        List<BeanId> beans = mongoOps.find(q, beanClass);
//        return new PropertyList<Id,Bean>(beans, beanDescriptor, "id");
    }

    @Override
    public Property<?> getContainerProperty(Object itemId, Object propertyId) {
        return getItem(itemId).getItemProperty(propertyId);
    }

    // return the data type of the given property id
    @Override
    public Class<?> getType(Object propertyId) {
        PropertyDescriptor pd = propertyDescriptorMap.get(propertyId);
        return pd == null? null: pd.getPropertyType();
    }

    @Override
    public int size() {
        return (int) mongoOps.count(query, beanClass);
    }

    @Override
    public boolean containsId(Object itemId) {
        return mongoOps.exists(Query.query(where(ID).is(itemId)), beanClass);
    }

    @Override
    public BeanItem<Bean> addItem(Object itemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("cannot addItem(); add directly to mongo");
    }

    @Override
    public Id addItem() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("cannot addItem(); add directly to mongo");
    }

    @Override
    public boolean removeItem(Object itemId) throws UnsupportedOperationException {
        mongoOps.findAndRemove(Query.query(where(ID).is(itemId)), beanClass);
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
        mongoOps.remove(Query.query(criteria), beanClass);
        return true;
    }

    @Override
    public int indexOfId(Object itemId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Id getIdByIndex(int index) {
        if (size() == 0) return null;
        List<Id> idList = getItemIds(index, 1);
        return idList.isEmpty()? null : idList.get(0);
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
        return getIdByIndex(0);
    }

    @Override
    public Id lastItemId() {
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


    private void assertIdValid(Object o) {
        if ( o == null )
            throw new NullPointerException("Id cannot be null");
        if ( o instanceof ObjectId ) // ! idClass.isInstance(o) )
            throw new IllegalArgumentException("Id is not an ObjectId: "+o.getClass());
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
