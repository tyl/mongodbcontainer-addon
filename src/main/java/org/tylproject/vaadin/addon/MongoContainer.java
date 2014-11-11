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
import org.tylproject.vaadin.addon.utils.Page;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by evacchi on 26/09/14.
 */
public class MongoContainer<Bean>
        extends AbstractContainer
        implements Container, Container.Ordered, Container.Indexed,
        Container.ItemSetChangeNotifier {


    public static class Builder {

        private final static int DEFAULT_PAGE_SIZE = 100;


        private final MongoOperations mongoOps;
        private Criteria mongoCriteria = new Criteria();
        private Class<?> beanClass;
        private int pageSize = DEFAULT_PAGE_SIZE;
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

        public Builder withPageSize(final int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder withProperty(Object id, Class<?> type) {
            ids.put(id, type);
            return this;
        }

        public <Bean> BufferedMongoContainer<Bean> buildBuffered() {
            BufferedMongoContainer<Bean> mc = new BufferedMongoContainer<Bean>(mongoCriteria, mongoOps, (Class<Bean>) beanClass, pageSize);
            return mc;
        }


        public <Bean> MongoContainer<Bean> build() {
            MongoContainer<Bean> mc= new MongoContainer<Bean>(mongoCriteria, mongoOps, (Class<Bean>) beanClass, pageSize);
            return mc;
        }

    }

    protected static final String ID = "_id";
    protected static final Logger log = Logger.getLogger("MongoContainer");


    @Nonnull private Page<ObjectId> page;
    protected final int pageSize;


    protected final Criteria criteria;
    protected final Query query;
    protected final MongoOperations mongoOps;

    protected final Class<Bean> beanClass;
    protected final BeanDescriptor beanDescriptor;

    protected final LinkedHashMap<String, PropertyDescriptor> propertyDescriptorMap;

    MongoContainer(final Criteria criteria,
                           final MongoOperations mongoOps,
                           final Class<Bean> beanClass,
                           final int pageSize) {
        this.criteria = criteria;
        this.query = Query.query(criteria);
        this.mongoOps = mongoOps;

        this.beanClass = beanClass;

        this.beanDescriptor = getBeanDescriptor(beanClass);
        this.propertyDescriptorMap = getBeanPropertyDescriptor(beanClass);

        this.pageSize = pageSize;

        fetchPage(0, pageSize);
    }


    private DBCursor cursor() {
        DBObject criteriaObject = criteria.getCriteriaObject();
        DBObject projectionObject = new BasicDBObject(ID, true);

        String collectionName = mongoOps.getCollectionName(beanClass);
        DBCollection dbCollection = mongoOps.getCollection(collectionName);

        // TODO: keep cursor around to possibly reuse
        DBCursor cursor = dbCollection.find(criteriaObject, projectionObject);
        return cursor;
    }

    private DBCursor cursorInRange(int skip, int limit) {
        return cursor().skip(skip).limit(limit);
    }

    private void fetchPage(int offset, int pageSize) {

        // TODO: keep cursor around to possibly reuse
        DBCursor cursor = cursorInRange(offset, pageSize);

        Page<ObjectId> newPage = new Page<ObjectId>(pageSize, offset, this.size());

        for (int i = offset; cursor.hasNext(); i++)
            newPage.set(i, (ObjectId) cursor.next().get(ID));

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

    public ObjectId addEntity(Bean target) {
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
        mongoOps.remove(Query.query(criteria), beanClass);
        return true;
    }

    @Override
    public int indexOfId(Object itemId) {
        ObjectId oid = assertIdValid(itemId);

        // for the principle of locality,
        // let us optimistically first check within the page
        int index = page.indexOf(oid);
        if (index > -1) return index;

        // otherwise, linearly scan the entire collection using a cursor
        // and only fetch the ids
        DBCursor cur = cursor();
        for (int i = 0; cur.hasNext(); i++) {
            // skip the check for those already in the page
            if (i >= page.offset && i < page.maxIndex) continue;
            if (cur.next().get(ID).equals(itemId)) return i;
        }
        return -1;
    }

    @Override
    @Nullable
    public ObjectId getIdByIndex(int index) {
        if (size() == 0) return null;
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
            ; // return the requested range
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

            // obj.getClass() is not really a getter
            propertyDescriptorMap.remove("class");

            return propertyDescriptorMap;
        } catch (Exception ex) { throw new Error(ex); }

    }

    private static Set<?> getVaadinPropertyIds(final Class<?> beanClass) throws IntrospectionException {
        LinkedHashMap<String,PropertyDescriptor> propDescrs = getBeanPropertyDescriptor(beanClass);
        return Collections.unmodifiableSet(propDescrs.keySet());
    }

    protected Field getIdField(Bean target) {
        for (Field f : beanClass.getDeclaredFields()) {
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
