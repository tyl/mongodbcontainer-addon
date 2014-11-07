package it.tylframework.vaadin.addon;

import com.vaadin.data.Buffered;
import com.vaadin.data.util.BeanItem;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.xml.transform.Source;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by evacchi on 06/11/14.
 */
public class BufferedMongoContainer<Bean> extends MongoContainer<Bean>
    implements Buffered {

    private final LinkedHashMap<ObjectId,BeanItem<Bean>> newItems = new LinkedHashMap<ObjectId, BeanItem<Bean>>();
    private final LinkedHashMap<ObjectId,BeanItem<Bean>> updatedItems = new LinkedHashMap<ObjectId, BeanItem<Bean>>();
    private final LinkedHashMap<ObjectId,BeanItem<Bean>> removedItems = new LinkedHashMap<ObjectId, BeanItem<Bean>>();

    BufferedMongoContainer(Criteria criteria, MongoOperations mongoOps, Class<Bean> beanClass) {
        super(criteria, mongoOps, beanClass);
    }

    @Override
    public void commit() throws SourceException {
        try {
            commitNewItems();
            commitUpdatedItems();
            commitRemovedItems();
        } catch (RuntimeException ex) {
            throw new SourceException(this, ex);
        }
    }

    private void commitRemovedItems() {
        for (ObjectId key: removedItems.keySet()) {
            Bean bean =  removedItems.get(key).getBean();
            mongoOps.remove(bean);
        }
    }

    private void commitUpdatedItems() {
        for (ObjectId key: updatedItems.keySet()) {
            Bean bean = updatedItems.get(key).getBean();
            mongoOps.save(bean);
        }
    }

    private void commitNewItems() {
        for (ObjectId key: newItems.keySet()) {
            Bean bean = newItems.get(key).getBean();
            mongoOps.insert(bean);
        }
    }

    @Override
    public void discard() throws SourceException {
        try {
            discardNewItems();
            discardUpdatedItems();
            discardRemovedItems();
        } catch (RuntimeException ex) {
            throw new SourceException(this, ex);
        }
    }

    private void discardRemovedItems() {
        removedItems.clear();
    }

    private void discardUpdatedItems() {
        updatedItems.clear();
    }

    private void discardNewItems() {
        updatedItems.clear();
    }

    @Override
    public void setBuffered(boolean buffered) {
        throw new UnsupportedOperationException(
                "A BufferedMongoContainer is always buffered");
    }

    @Override
    public boolean isBuffered() { return true; }

    @Override
    public boolean isModified() {
        return !newItems.isEmpty()
               || !updatedItems.isEmpty()
               || !removedItems.isEmpty();
    }


    @Override
    public BeanItem<Bean> addItem(Object itemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId addItem() throws UnsupportedOperationException {
        try {

            Bean bean   = beanClass.newInstance();
            return this.addDocument(bean);

        } catch (InstantiationException ex) {
            throw new UnsupportedOperationException(
                    "the given id or bean class cannot be instantiated.", ex);
        } catch (IllegalAccessException ex) {
            throw new UnsupportedOperationException(
                    "the given id or bean class or its nullary constructor " +
                            "is not accessible.", ex);
        }
    }

    @Override
    public ObjectId addDocument(Bean target) {
        BeanItem<Bean> beanItem = new BeanItem<Bean>(target);
        ObjectId id = injectId(target);
        newItems.put(id, beanItem);
        return id;
    }

    protected ObjectId injectId(Bean target) {
        try {
            ObjectId id = new ObjectId();
            getIdField(target).set(target, id);
            return id;
        } catch (IllegalAccessException ex) { throw new UnsupportedOperationException(ex); }
    }
}
