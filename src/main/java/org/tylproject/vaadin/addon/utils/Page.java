/**
 * Copyright (c) 2014 - Marco Pancotti, Edoardo Vacchi and Daniele Zonca
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
    private int maxValidIndex;
    private boolean valid;
    private T[] values;

    public Page(int pageSize, int offset, int collectionSize) {
        this.pageSize = pageSize;
        this.offset = offset;
        this.size = collectionSize;
        this.valid = true;
        this.maxIndex = offset+pageSize;
        this.maxValidIndex = 0;
        this.values = (T[]) new Object[pageSize];
        Arrays.fill(values, null);
    }

    public void set(int index, T value) {
        if (index < offset)
            throw new ArrayIndexOutOfBoundsException(index+"<"+ offset);
        if (index > this.maxIndex)
            throw new ArrayIndexOutOfBoundsException(index+">"+ maxIndex);

        int actualIndex = index-offset;
        this.values[actualIndex] = value;

        if (index > maxValidIndex)
            maxValidIndex = index;

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
                && startIndex + numberOfItems <= this.maxValidIndex;
    }

    public List<T> subList(int startIndex, int numberOfItems) {
        List<T> idList = this.toImmutableList(); // indexed from 0, as required by the interface contract
        return idList.subList(startIndex-offset, numberOfItems);
    }

}
