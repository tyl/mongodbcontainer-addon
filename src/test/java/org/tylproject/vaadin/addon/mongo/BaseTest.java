package org.tylproject.vaadin.addon.mongo;

import com.mongodb.MongoClient;
import org.junit.After;
import org.junit.Before;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.tylproject.data.mongo.Customer;
import org.tylproject.vaadin.addon.MongoContainer;

/**
 * Created by evacchi on 09/01/15.
 */
public class BaseTest {


    protected final MongoOperations mongoOps;
    protected final Class<Customer> beanClass = Customer.class;

    public BaseTest() {
        try {
            this.mongoOps = new MongoTemplate(new MongoClient(), "database");
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public MongoContainer.Builder<Customer> builder() {
        return MongoContainer.Builder.forEntity(beanClass,mongoOps)
                .withPageSize(3)
                .sortedBy(new Sort("firstName"));
    }


    @Before
    public void setupDatabase() throws Exception {
        // save some customers
        mongoOps.save(new Customer("Austin", "Carlson"));
        mongoOps.save(new Customer("Austin", "Scott"));
        mongoOps.save(new Customer("Cordelia", "McDaniel"));
        mongoOps.save(new Customer("Herbert", "Harris"));
        mongoOps.save(new Customer("Jimmy", "Simpson"));
        mongoOps.save(new Customer("Keith", "George"));
        mongoOps.save(new Customer("Susan", "Long"));
    }


    public void removeAll() {
        for (Customer c: mongoOps.findAll(Customer.class))
            mongoOps.remove(c);
    }

    @After
    public void teardownDatabase() {
        removeAll();
    }
}
