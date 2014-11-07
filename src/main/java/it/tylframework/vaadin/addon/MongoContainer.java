package it.tylframework.vaadin.addon;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import it.tylframework.vaadin.addon.utils.Page;
import org.bson.types.ObjectId;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Nonnull;
import java.beans.*;
import java.lang.reflect.Field;
import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by evacchi on 26/09/14.
 */
public class MongoContainer<Bean>
        //extends AbstractContainer
        implements Container, Container.Ordered, Container.Indexed {


    public static class Builder {
        private final MongoOperations mongoOps;
        private Criteria mongoCriteria = new Criteria();
        private Class<?> beanClass;
        private Class<?> idClass = ObjectId.class;
        private boolean buffered = false;
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

        public Builder withProperty(Object id, Class<?> type) {
            ids.put(id, type);
            return this;
        }

        public Builder buffered() {
            this.buffered = true;
            return this;
        }

        public <Bean> MongoContainer<Bean> build() {
            MongoContainer<Bean> mc;
            if (buffered) {
                mc = new BufferedMongoContainer<Bean>(mongoCriteria, mongoOps, (Class<Bean>) beanClass);
            } else {
                mc = new MongoContainer<Bean>(mongoCriteria, mongoOps, (Class<Bean>) beanClass);
            }
            for (Object id: ids.keySet()) {
                mc.addContainerProperty(id, ids.get(id), null);
            }
            return mc;
        }

    }

    private final static int DEFAULT_PAGE_SIZE = 100;

    @Nonnull private Page<ObjectId> page;

    protected static final String ID = "_id";

    protected final Criteria criteria;
    protected final Query query;
    protected final MongoOperations mongoOps;

    protected final Class<Bean> beanClass;
    protected final BeanDescriptor beanDescriptor;

    protected final LinkedHashMap<String, PropertyDescriptor> propertyDescriptorMap;

    MongoContainer(final Criteria criteria,
                           final MongoOperations mongoOps,
                           final Class<Bean> beanClass) {
        this.criteria = criteria;
        this.query = Query.query(criteria);
        this.mongoOps = mongoOps;

        this.beanClass = beanClass;

        this.beanDescriptor = getBeanDescriptor(beanClass);
        this.propertyDescriptorMap = getBeanPropertyDescriptor(beanClass);

        fetchPage(0, DEFAULT_PAGE_SIZE);
    }

    private void fetchPage(int offset, int pageSize) {
        Page<ObjectId> newPage = new Page<ObjectId>(pageSize, offset, this.size());

        DBObject criteriaObject = criteria.getCriteriaObject();
        DBObject projectionObject = new BasicDBObject(ID, true);

        String collectionName = mongoOps.getCollectionName(beanClass);
        DBCollection dbCollection = mongoOps.getCollection(collectionName);

        // TODO: keep cursor around to possibly reuse
        DBCursor cursor = dbCollection.find(criteriaObject, projectionObject);

        for (int i = 0; i < offset; i++) {
            cursor.next();
        }

        for (int i = offset; i < pageSize && cursor.hasNext(); i++) {
            DBObject value = cursor.next();
            newPage.set(i, (ObjectId) value.get("_id"));
        }

        this.page = newPage;
    }

    static class BeanId { @Id public ObjectId _id; }
    BeanDescriptor beanIdDescriptor = getBeanDescriptor(BeanId.class);

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
    public List<ObjectId> getItemIds() {
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
        throw new UnsupportedOperationException(
                "cannot addItem(); insert() into mongo or build a buffered container");
    }

    @Override
    public ObjectId addItem() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "cannot addItem(); insert() into mongo or build a buffered container");
    }

    public ObjectId addDocument(Bean target) {
        mongoOps.insert(target);
        try {
            return (ObjectId) getIdField(target).get(target);
        } catch (IllegalAccessException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    @Override
    public boolean removeItem(Object itemId) throws UnsupportedOperationException {
        mongoOps.findAndRemove(Query.query(where(ID).is(itemId)), beanClass);
        page.setInvalid();
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
    public ObjectId getIdByIndex(int index) {
        if (size() == 0) return null;
        List<ObjectId> idList = getItemIds(index, 1);
        return idList.isEmpty()? null : idList.get(0);
    }

    @Override
    public List<ObjectId> getItemIds(int startIndex, int numberOfItems) {
        //List<BeanId> beans = mongoOps.find(Query.query(criteria).skip(startIndex).limit(numberOfItems), BeanId.class);
        //List<ObjectId> ids = new PropertyList<ObjectId,BeanId>(beans, beanIdDescriptor, "_id");

        if (!page.isInvalid() && startIndex >= page.offset && numberOfItems <= page.pageSize) {
            return this.page.toImmutableList().subList(startIndex, startIndex+numberOfItems);
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
        List<ObjectId> itemIds = getItemIds();
        return itemIds.get(itemIds.indexOf(itemId) + 1);
    }

    @Override
    public ObjectId prevItemId(Object itemId) {
        List<ObjectId> itemIds = getItemIds();
        return itemIds.get(itemIds.indexOf(itemId)-1);
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


    private void assertIdValid(Object o) {
        if ( o == null )
            throw new NullPointerException("Id cannot be null");
        if ( ! ( o instanceof ObjectId ) )
            throw new IllegalArgumentException("Id is not instance of ObjectId: "+o);
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

    protected Field getIdField(Bean target) {
        for (Field f : beanClass.getDeclaredFields()) {
            System.out.println(f);
            if (f.isAnnotationPresent(org.springframework.data.annotation.Id.class)) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new UnsupportedOperationException("no id field was found");
    }

    /*
    String getCollectionName(@Nonnull Class<?> beanClass) {
        MongoPersistentEntity<?> persistentEntity = mongoOps.getConverter().getMappingContext().getPersistentEntity(beanClass);
        if (persistentEntity == null) {
            throw new IllegalArgumentException(
                    "Cannot find collection for the given class"
                    + beanClass.getName());
        }
        return persistentEntity.getCollection();
    }
    */
}
