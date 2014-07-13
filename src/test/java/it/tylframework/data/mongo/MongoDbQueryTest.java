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

import com.vaadin.data.util.BeanItem;
import it.tylframework.addon.MongoDbQuery;
import it.tylframework.addon.MongoDbQueryDefinition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.OutputCapture;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by marco on 12/07/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleMongoApplication.class)
public class MongoDbQueryTest {

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Autowired
    private CustomerRepository repository;

    @Before
    public void setupDatabase(){
        repository.deleteAll();

        // save a couple of customers
        repository.save(new Customer("Andrea", "Novara"));
        repository.save(new Customer("Edoardo", "Vacchi"));
        repository.save(new Customer("Marco", "Pancotti"));
        repository.save(new Customer("Alessandro", "Mongelli"));
        repository.save(new Customer("Michele", "Sciabarra"));
        repository.save(new Customer("Adriano", "Marchetti"));
        repository.save(new Customer("Luca", "Buraggi"));
    }

    @Test
    public void testSize(){
        MongoDbQueryDefinition mdqd = new MongoDbQueryDefinition(false, Customer.class, 20, Customer.class);
        MongoDbQuery mdq = new MongoDbQuery<Customer>(mdqd,repository);
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
        MongoDbQueryDefinition mdqd = new MongoDbQueryDefinition(false, Customer.class, 20, Customer.class);
        MongoDbQuery mdq = new MongoDbQuery<Customer>(mdqd, repository);
        List<BeanItem> l = mdq.loadItems(1, 3);
        for (BeanItem i : l) {
            Customer c = (Customer) i.getBean();
            System.out.println("Nome: " + c.getFirstName() + " " + c.getLastName());
        }
        String output = this.outputCapture.toString();
        assertTrue("Wrong output: " + output,
                output.contains("Nome: Michele Sciabarra"));
    }
}
