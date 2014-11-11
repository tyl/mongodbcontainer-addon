package org.tylproject.vaadin.addon;

import com.vaadin.data.util.BeanItem;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.AbstractList;
import java.util.List;

/**
 * Created by evacchi on 26/09/14.
 */
public class PropertyList<Property,Bean> extends AbstractList<Property> {
    private final BeanDescriptor beanDescriptor;

    private final List<Bean> beans;
    private final String propertyId;

    public PropertyList(List<Bean> beans, BeanDescriptor beanDescriptor, String propertyId) {
        this.propertyId = propertyId;
        this.beans = beans;

        this.beanDescriptor = beanDescriptor;
    }

    @Override
    public Property get(int index) {
        Bean b = beans.get(index);
        return (Property) new BeanItem<Bean>(b).getItemProperty(propertyId).getValue();
    }

    @Override
    public int size() {
        return beans.size();
    }
}
