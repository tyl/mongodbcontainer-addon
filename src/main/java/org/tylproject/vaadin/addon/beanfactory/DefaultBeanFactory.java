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

 package org.tylproject.vaadin.addon.beanfactory;

import org.bson.types.ObjectId;

import java.lang.reflect.Field;

/**
 * Created by evacchi on 14/11/14.
 */
public class DefaultBeanFactory<T> implements BeanFactory<T> {
    Class<T> beanClass;

    public DefaultBeanFactory(Class<T> beanClass) {
        this.beanClass = beanClass;
    }

    /**
     * attempts to inject a new {@see ObjectId} instance in
     * the field annotated with @Id
     * @param target
     * @return the injected id
     * @throws java.lang.UnsupportedOperationException
     *          if {@link java.lang.reflect.Field#set(Object, Object)} throws
     *          an {@link java.lang.IllegalAccessException}
     */
    @Override
    public ObjectId injectId(T target) {
        try {
            ObjectId id = new ObjectId();
            getIdField().set(target, id);
            return id;
        } catch (IllegalAccessException ex) { throw new UnsupportedOperationException(ex); }
    }

    @Override
    public T newInstance() {
        try {
            return beanClass.newInstance();
        } catch (InstantiationException ex) {
            throw new UnsupportedOperationException(
                    "the given id or bean class cannot be instantiated.", ex);
        } catch (IllegalAccessException ex) {
            throw new UnsupportedOperationException(
                    "the given id or bean class or its nullary constructor " +
                            "is not accessible.", ex);
        }
    }

    /**
     * @param target the target object instance
     * @return the generated ObjectId
     * @throws java.lang.UnsupportedOperationException
     *          if {@link java.lang.reflect.Field#set(Object, Object)} throws
     *          an {@link java.lang.IllegalAccessException}
     */
    @Override
    public ObjectId getId(T target) {
        try {
            return (ObjectId) getIdField().get(target);
        } catch (IllegalAccessException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    protected Field getIdField() {
        for (Field f : beanClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(org.springframework.data.annotation.Id.class)) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new UnsupportedOperationException("no id field was found");
    }
}
