/*
 * Copyright (c) 2014 - Marco Pancotti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.tylframework.data.mongo;

import com.mongodb.Mongo;
import com.vaadin.data.util.BeanItem;
import it.tylframework.addon.MongoQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.OutputCapture;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;

import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by marco on 12/07/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleMongoApplication.class)
public class MongoQueryTest {

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    //@Autowired
    private final MongoOperations mongoOps;

    private final Class<Customer> beanClass = Customer.class;

    public MongoQueryTest() throws Exception {
        mongoOps = new MongoTemplate(new Mongo(), "database");

    }


    @Before
    public void setupDatabase() throws Exception {
        for (Customer c: mongoOps.findAll(Customer.class))
            mongoOps.remove(c);

        // save some customers
        mongoOps.save(new Customer("Andrea", "Novara"));
        mongoOps.save(new Customer("Edoardo", "Vacchi"));
        mongoOps.save(new Customer("Marco", "Pancotti"));
        mongoOps.save(new Customer("Alessandro", "Mongelli"));
        mongoOps.save(new Customer("Michele", "Sciabarra"));
        mongoOps.save(new Customer("Adriano", "Marchetti"));
        mongoOps.save(new Customer("Luca", "Buraggi"));
    }

    @Test
    public void testSize(){
        MongoQuery mdq = new MongoQuery<Customer>(mongoOps, new Query(), beanClass);
        System.out.println("Size del container="+mdq.size());
        String output = this.outputCapture.toString();
        assertTrue("Wrong output: " + output,
                output.contains("Size del container=7"));
    }

    // fetch using Container
    @Test
    public void testLoadItems() {
        System.out.println("Customers found with LazyBeanContainer():");
        System.out.println("-------------------------------");
        MongoQuery mdq = new MongoQuery<Customer>(mongoOps, new Query(), beanClass);
        List<BeanItem> l = mdq.loadItems(1, 3);
        for (BeanItem i : l) {
            Customer c = (Customer) i.getBean();
            System.out.println("Nome: " + c.getFirstName() + " " + c.getLastName());
        }
        String output = this.outputCapture.toString();
        assertFalse("Wrong output: " + output,
                output.contains("Nome: Michele Sciabarra")); // Sciabarra is fourth
    }
}
