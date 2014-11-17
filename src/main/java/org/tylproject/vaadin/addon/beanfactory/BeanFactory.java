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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A factory for beans
 *
 * @param <T> the type of the bean
 */
public interface BeanFactory<T> {

    /**
     * injects the given ObjectId into the target bean
     */
    ObjectId injectId(T target);

    /**
     * instantiates and returns a bean of the given
     * type parameter
     */
    T newInstance();

    /**
     * returns the id of the given bean
     */
    ObjectId getId(T target);
}
