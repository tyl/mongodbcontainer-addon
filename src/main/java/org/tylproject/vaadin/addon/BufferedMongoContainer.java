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

import com.mongodb.DBCursor;
import com.vaadin.data.Buffered;
import com.vaadin.data.util.BeanItem;
import org.bson.types.ObjectId;
import org.tylproject.vaadin.addon.utils.Page;

import javax.annotation.Nullable;
import java.util.*;

public class BufferedMongoContainer<Bean> extends MongoContainer<Bean>
    implements Buffered {

    private final LinkedHashMap<ObjectId,BeanItem<Bean>> newItems = new LinkedHashMap<ObjectId, BeanItem<Bean>>();
    private final LinkedHashMap<ObjectId,BeanItem<Bean>> updatedItems = new LinkedHashMap<ObjectId, BeanItem<Bean>>();
    private final LinkedHashMap<ObjectId,BeanItem<Bean>> removedItems = new LinkedHashMap<ObjectId, BeanItem<Bean>>();
    private final TreeSet<Integer> removedItemsIndices = new TreeSet<Integer>();

    BufferedMongoContainer(Builder<Bean> bldr) {
        super(bldr);
    }

    @Override
    public void commit() throws SourceException {
        try {
            commitNewItems();
            commitUpdatedItems();
            commitRemovedItems();
            clearBuffers();
            fireItemSetChange();
        } catch (RuntimeException ex) {
            throw new SourceException(this, ex);
        }
    }

    private void clearBuffers() {
        for (LinkedHashMap<ObjectId,BeanItem<Bean>> temporaryStorage:
                Arrays.asList(newItems, updatedItems, removedItems)) {
            temporaryStorage.clear();
        }
        removedItemsIndices.clear();
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
            clearBuffers();
            fireItemSetChange();
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
    public BeanItem<Bean> getItem(Object itemId) {
        ObjectId id = assertIdValid(itemId);

        // if the internal buffers are not empty,
        // first check there
        if (this.isModified()) {
            for (LinkedHashMap<ObjectId, BeanItem<Bean>> temporaryStorage :
                    Arrays.asList(newItems, updatedItems)) {
                if (temporaryStorage.containsKey(id))
                    return temporaryStorage.get(id);
            }
            // if removed, it should return null
            // as the id would not found in the collection
            if (removedItems.containsKey(itemId)) return null;
        }

        // otherwise, just return the item with the usual strategy
        return super.getItem(itemId);
    }

    @Override
    @Nullable
    public ObjectId getIdByIndex(final int index) {
        if (index < 0 || this.size() == 0) return null;

        int actualIndex = index;

        if (!removedItems.isEmpty() && index >= removedItemsIndices.first()) {
            // number of elements to skip
            int skipElements = removedItemsIndices.subSet(
                    removedItemsIndices.first(), true,
                    removedItemsIndices.floor(index), true).size();
            actualIndex += skipElements;
        }

        DBCursor cur = cursorInRange(actualIndex, 1);
        return cur.hasNext()?
                (ObjectId)cur.next().get(ID)
                : null;
    }

    @Override
    public boolean removeItem(Object itemId) throws
            UnsupportedOperationException {
        if (newItems.containsKey(itemId)) {
            newItems.remove(itemId);
        } else {
            if (updatedItems.containsKey(itemId)) {
                updatedItems.remove(itemId);
            }
            int index = super.indexOfId(itemId);
            removedItems.put((ObjectId) itemId, super.getItem(itemId));
            removedItemsIndices.add(index);
        }
        fireItemSetChange();
        return true;
    }

    @Override
    public int indexOfId(Object itemId) {
        if (newItems.containsKey(itemId)) {
            int i = 0;
            for (ObjectId newId: newItems.keySet()) {
                if (itemId.equals(newId)) {
                    return super.size() - removedItems.size() + i;
                }
                i++;
            }
        }
        if (removedItems.containsKey(itemId)) return -1;
        return super.indexOfId(itemId);
    }

    @Override
    public boolean containsId(Object itemId) {
        if (removedItems.containsKey(itemId)) return false;
        else if (newItems.containsKey(itemId)) return true;
        else return super.containsId(itemId);
    }

    @Override
    public int size() {
        int actualSize = super.size();
        if (newItems.isEmpty() && removedItems.isEmpty()) return actualSize;
        else return actualSize + newItems.size() - removedItems.size();
    }

    @Override
    protected void fetchPage(int offset, int pageSize) {

        // we'll have to skip N = removedItems.size()
        // so, ensure to get enough elements
        // by fetching pageSize + removedItems.size()
        DBCursor cursor = cursorInRange(offset, pageSize + removedItems.size());

        // the page size does not change, though
        Page<ObjectId> newPage = new Page<ObjectId>(pageSize, offset, this.size());

        // stop when cursor has reached the end OR index >= newPage.maxIndex,
        // whichever occurs first
        int index = offset;
        for (   ; cursor.hasNext() && index < newPage.maxIndex; ) {
            ObjectId objectId = (ObjectId) cursor.next().get(ID);
            // if the element has been scheduled for removal, skip it
            if (removedItems.containsKey(objectId)) continue;
            newPage.set(index, objectId);
            index++;
        }

        // if there is still space left in the page,
        // fill it with elements from addedItems
        if ( ( newPage.maxIndex >= super.size() - removedItems.size() )
                && index < newPage.maxIndex) {
            for (ObjectId objectId: newItems.keySet()) {
                if (index >= newPage.maxIndex) break;
                newPage.set(index, objectId);
                index++;
            }
        }

        this.page = newPage;
    }

    /**
     * Notify the container that the given itemId will be updated
     *
     * @param itemId
     * @throws java.lang.IllegalArgumentException if the itemId does not exist
     *           or it has been scheduled for removal
     *
     * @return the item that will be updated
     */
    public BeanItem<Bean> updateItem(ObjectId itemId) {

        if (this.removedItems.containsKey(itemId))
            throw new IllegalArgumentException("item "+itemId+" was removed");
        if (!this.newItems.containsKey(itemId)) {
            if (!this.containsId(itemId)) {
                throw new IllegalArgumentException("item " + itemId + " was removed");
            }

            page.setInvalid();

            BeanItem<Bean> updatedItem = getItem(itemId);
            this.updatedItems.put(itemId, updatedItem);

            return updatedItem;
        } else {
            return this.newItems.get(itemId);
        }

    }

    @Override
    public BeanItem<Bean> addItem(Object itemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Instantiate an entity automatically and return its ObjectId
     *
     * This method tries to instantiate an entity automatically
     * using the {@link org.tylproject.vaadin.addon.beanfactory.BeanFactory}
     * and insert it in the pending list of added items.
     *
     */
    @Override
    public ObjectId addItem() throws UnsupportedOperationException {
        return this.addEntity(beanFactory.newInstance());
    }

    /**
     * schedules the given bean for adding
     */
    @Override
    public ObjectId addEntity(Bean target) {
        BeanItem<Bean> beanItem = new BeanItem<Bean>(target, simpleProperties.keySet());
        for (String nestedProp: this.nestedProperties.keySet())
            beanItem.addNestedProperty(nestedProp);

        ObjectId id = beanFactory.injectId(target);
        newItems.put(id, beanItem);
        fireItemSetChange();
        return id;
    }

}
