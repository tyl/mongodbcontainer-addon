package org.tylproject.vaadin.addon.beanfactory;

import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Created by evacchi on 14/11/14.
 */
public interface BeanFactory<T> {

    ObjectId injectId(T target);

    T newInstance();

    ObjectId getId(T target);
}
