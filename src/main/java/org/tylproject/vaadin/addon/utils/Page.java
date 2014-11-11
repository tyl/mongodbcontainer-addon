package org.tylproject.vaadin.addon.utils;

import java.text.MessageFormat;
import java.util.*;

/**
 * Created by evacchi on 07/11/14.
 */
public class Page<T> {
    public final int pageSize;
    public final int offset;
    public final int size;
    public final int maxIndex;
    private boolean valid;
    private T[] values;

    public Page(int pageSize, int offset, int collectionSize) {
        this.pageSize = pageSize;
        this.offset = offset;
        this.size = collectionSize;
        this.valid = true;
        this.maxIndex = offset+pageSize;
        this.values = (T[]) new Object[pageSize];
        Arrays.fill(values, null);
    }

    public void set(int index, T value) {
        if (index < offset)
            throw new ArrayIndexOutOfBoundsException(index+"<"+ offset);
        int actualIndex = index-offset;
        this.values[actualIndex] = value;
    }

    public T get(int index) {
        if (index < offset || index > offset+pageSize)
            throw new IndexOutOfBoundsException(
                    MessageFormat.format(
                            "index {} not within bounds [{},{}]",
                            offset, size));

        return this.values[index-offset];
    }

    public int indexOf(T value) {
        for (int i = 0; i < values.length; ++i) {
            if (value.equals(values[i])) {
                return i + offset;
            }
        }
        return -1;
    }

    public int getPageSize() {
        return pageSize;
    }


    public List<T> toImmutableList() {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    public void setInvalid() {
        this.valid = false;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isWithinRange(int startIndex, int numberOfItems) {
        return startIndex >= this.offset
                //&& numberOfItems <= this.pageSize
                && startIndex + numberOfItems <= this.size;
    }

    public List<T> subList(int startIndex, int numberOfItems) {
        List<T> idList = this.toImmutableList(); // indexed from 0, as required by the interface contract
        return idList.subList(startIndex-offset, numberOfItems);
    }

}
