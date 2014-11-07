package it.tylframework.vaadin.addon.utils;

import java.text.MessageFormat;
import java.util.*;

/**
 * Created by evacchi on 07/11/14.
 */
public class Page<T> {
    public final int pageSize;
    public final int offset;
    public final int size;
    private T[] values;
    private boolean invalid;

    public Page(int pageSize, int offset, int collectionSize) {
        this.pageSize = pageSize;
        this.offset = offset;
        this.size = collectionSize;
        this.values = (T[]) new Object[pageSize];
        Arrays.fill(values, null);
    }

    public void set(int index, T value) {
        int actualIndex = offset+index;
        this.values[actualIndex] = value;
    }

    public T get(int index) {
        if (index < offset || index > size)
            throw new IndexOutOfBoundsException(
                    MessageFormat.format(
                            "index {} not within bounds [{},{}]",
                            offset, size));

        return this.values[offset+index];
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
        this.invalid = true;
    }

    public boolean isInvalid() {
        return invalid;
    }

}
