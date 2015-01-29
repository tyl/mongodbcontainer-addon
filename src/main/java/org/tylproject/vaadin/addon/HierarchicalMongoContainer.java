package org.tylproject.vaadin.addon;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.vaadin.data.Container;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by evacchi on 29/01/15.
 */
public class HierarchicalMongoContainer<Bean> extends MongoContainer<Bean> implements Container.Hierarchical {

    private final String parentProperty;
    private Object lastRequestedItemId;
    private Collection<ObjectId> lastRequestedChildren;


    public HierarchicalMongoContainer(Builder<Bean> bldr) {
        super(bldr);
        this.parentProperty = bldr.parentProperty;

    }

    @Override
    public boolean areChildrenAllowed(Object itemId) {
        return getChildren(itemId).size() > 0;
    }

    @Override
    public Collection<ObjectId> getChildren(Object itemId) {
        assertIdValid(itemId);

        if (lastRequestedItemId == itemId) return lastRequestedChildren;

        lastRequestedItemId = itemId;

        DBObject parentCriteria = new BasicDBObject();
        parentCriteria.put(parentProperty, itemId);
        ArrayList<ObjectId> ids = new ArrayList<ObjectId>();
        DBCursor cur = cursor(parentCriteria);

        while (cur.hasNext()) {
            ids.add((ObjectId) cur.next().get(ID));
        }

        lastRequestedChildren = ids;

        return ids;
    }

    @Override
    public ObjectId getParent(Object itemId) {
        return (ObjectId) getItem(itemId).getItemProperty(parentProperty).getValue();
    }

    @Override
    public Collection<ObjectId> rootItemIds() {
        DBObject parentCriteria = new BasicDBObject();
        parentCriteria.put(parentProperty, null);
        ArrayList<ObjectId> ids = new ArrayList<ObjectId>();
        DBCursor cur = cursor(parentCriteria);

        while (cur.hasNext()) {
            ids.add((ObjectId) cur.next().get(ID));
        }

        return ids;

    }

    @Override
    public boolean setParent(Object itemId, Object newParentId) throws
            UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws
            UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRoot(Object itemId) {
        return getParent(itemId) == null;
    }

    @Override
    public boolean hasChildren(Object itemId) {
        if (lastRequestedItemId == itemId) return lastRequestedChildren.size() > 0;
        else return getChildren(itemId).size() > 0;
    }
}
