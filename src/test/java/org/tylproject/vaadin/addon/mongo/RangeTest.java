package org.tylproject.vaadin.addon.mongo;

import com.mongodb.MongoClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.tylproject.data.mongo.Customer;
import org.tylproject.vaadin.addon.MongoContainer;

import static junit.framework.Assert.assertEquals;

/**
 * Created by evacchi on 11/02/15.
 */
public class RangeTest {


    protected final MongoOperations mongoOps;
    protected final Class<Customer> beanClass = Customer.class;

    public RangeTest() {
        try {
            this.mongoOps = new MongoTemplate(new MongoClient(), "database");
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public MongoContainer.Builder<Customer> builder() {
        return MongoContainer.Builder.forEntity(beanClass, mongoOps)
                .sortedBy(new Sort("firstName"));
    }

    @Before
    public void setup() {
        for (int i = 0; i < 200; i++){
            mongoOps.insert(new Customer("John_"+i, "Doe"));
        }
    }
    @After
    public void tearDown() {
        mongoOps.dropCollection(Customer.class);
    }

    @Test
    public void testSubList() {


            final MongoContainer<Customer> mc = builder().build();
            assertEquals(15, mc.getItemIds(0, 15).size());
            assertEquals(10, mc.getItemIds(41, 10).size());
            assertEquals(10, mc.getItemIds(23, 10).size());

    }
}
