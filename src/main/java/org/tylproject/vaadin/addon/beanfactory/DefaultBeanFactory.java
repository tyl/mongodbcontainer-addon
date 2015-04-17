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

 package org.tylproject.vaadin.addon.beanfactory;

import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A bean factory that reflectively reads and injects
 * an ObjectId into a target bean instance.
 *
 */
public class DefaultBeanFactory<T> implements BeanFactory<T> {
    Class<T> beanClass;

    public DefaultBeanFactory(Class<T> beanClass) {
        this.beanClass = beanClass;
    }

    /**
     * attempts to inject a new {@link ObjectId} instance in
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
            setIdValue(target, id);
            return id;
        } catch (ReflectiveOperationException ex) { throw new UnsupportedOperationException(ex); }
    }

    /**
     *
     * @throws java.lang.UnsupportedOperationException
     *          if the instantiation throws an {@link InstantiationException}
     *          or an {@link IllegalAccessException}
     */
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
            return getIdValue(target);
        } catch (ReflectiveOperationException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    /**
     * @throws java.lang.UnsupportedOperationException
     *          if no field is annotated using
     *          {@link org.springframework.data.annotation.Id}
     */
    protected ObjectId getIdValue(T target) throws ReflectiveOperationException {

        for (Field f : beanClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(org.springframework.data.annotation.Id.class)) {
                f.setAccessible(true);
                return (ObjectId) f.get(target);
            }
        }
        for (Method m: beanClass.getMethods()) {
            if (m.getName().equals("getId")) {
                return (ObjectId) m.invoke(target);
            }
        }
        throw new UnsupportedOperationException("no id field was found");
    }


    /**
     * @throws java.lang.UnsupportedOperationException
     *          if no field is annotated using
     *          {@link org.springframework.data.annotation.Id}
     */
    protected void setIdValue(T target, ObjectId value) throws ReflectiveOperationException {

        for (Field f : beanClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(org.springframework.data.annotation.Id.class)) {
                f.setAccessible(true);
                f.set(target, value);
                return;
            }
        }
        for (Method m: beanClass.getMethods()) {
            if (m.getName().equals("setId")) {
                m.invoke(target, value);
                return;
            }
        }
        throw new UnsupportedOperationException("no id field was found");
    }
}
