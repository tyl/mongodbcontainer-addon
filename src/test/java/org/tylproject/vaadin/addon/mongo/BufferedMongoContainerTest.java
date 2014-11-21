package org.tylproject.vaadin.addon.mongo;

import com.mongodb.MongoClient;
import junit.framework.Assert;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.tylproject.data.mongo.Customer;
import org.tylproject.vaadin.addon.BufferedMongoContainer;
import org.tylproject.vaadin.addon.MongoContainer;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by evacchi on 12/11/14.
 */
public class BufferedMongoContainerTest {

    private final MongoTemplate mongoOps;
    private final Class<Customer> beanClass = Customer.class;

    public BufferedMongoContainerTest() throws Exception {
        this.mongoOps = new MongoTemplate(new MongoClient(), "database");
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
        mongoOps.save(new Customer("Callie", "Scott"));
        mongoOps.save(new Customer("Cordelia", "McDaniel"));
        mongoOps.save(new Customer("Herbert", "Harris"));
        mongoOps.save(new Customer("Jimmy", "Simpson"));
        mongoOps.save(new Customer("Keith", "George"));
        mongoOps.save(new Customer("Susan", "Long"));
    }


    @Test
    public void testRemoveFirstItem() {
        final BufferedMongoContainer<Customer> mc =
                builder().buildBuffered();

        int initSize = mc.size();

        ObjectId itemId = mc.firstItemId();
        mc.removeItem(itemId);
        ObjectId nextItemId = mc.firstItemId();
        assertNotNull(nextItemId);
        assertNotEquals(itemId, nextItemId);
        mc.removeItem(nextItemId);

        assertEquals(-1, mc.indexOfId(itemId));
        assertFalse(mc.getItemIds(0, 1).contains(nextItemId));
        assertNotEquals(null, mc.getItemIds(0, 1).get(0));
        assertNotEquals(initSize, mc.size());
    }

}
