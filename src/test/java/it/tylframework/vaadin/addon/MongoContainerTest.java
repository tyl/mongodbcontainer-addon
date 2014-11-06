package it.tylframework.vaadin.addon;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.vaadin.data.util.BeanItem;
import it.tylframework.addon.MongoQuery;
import it.tylframework.data.mongo.Customer;
import it.tylframework.data.mongo.SampleMongoApplication;
import junit.framework.TestCase;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.Serializable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Created by evacchi on 26/09/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleMongoApplication.class)

public class MongoContainerTest {
    private final MongoOperations mongoOps;
    private final Class<Customer> beanClass = Customer.class;

    public MongoContainerTest() throws Exception {
        this.mongoOps = new MongoTemplate(new MongoClient(), "database");
    }

    public MongoContainer.Builder builder() {

        return MongoContainer.Builder.with(mongoOps)
                .withBeanClass(beanClass);

    }

    @Before
    public void setupDatabase() throws Exception {
        // save some customers
        mongoOps.save(new Customer("Andrea", "Novara"));
        mongoOps.save(new Customer("Edoardo", "Vacchi"));
        mongoOps.save(new Customer("Marco", "Pancotti"));
        mongoOps.save(new Customer("Alessandro", "Mongelli"));
        mongoOps.save(new Customer("Michele", "Sciabarra"));
        mongoOps.save(new Customer("Adriano", "Marchetti"));
        mongoOps.save(new Customer("Luca", "Buraggi"));
    }



    @After
    public void teardownDatabase() {
        for (Customer c: mongoOps.findAll(Customer.class))
            mongoOps.remove(c);
    }


    @Test
    public void testSize(){
        assertEquals(builder().forCriteria(new Criteria()).build().size(), 7);
    }

//
//    @Test
//    public void testIdList() {
//        final MongoContainer<Customer,Serializable> mc = mongoContainerBuilder.forCriteria(new Criteria()).build();
//        System.out.println(mc.getItemIds());
//        assertEquals(mc.getItemIds().size(), 7);
//    }

    // fetch using Container
    @Test
    public void testLoadItems() {

        final Criteria crit = where("firstName").regex(".*d.*");

        final MongoContainer<ObjectId,Customer> mc =
                builder()
                        .forCriteria(crit)
                        .build();


        List<Customer> bs = mongoOps.find(query(crit), beanClass);

        for (Customer c: bs) {
            System.out.println(c);
            assertEquals(true, mc.containsId(c.getId()));
        }

        assertEquals(4, mc.size());
    }

    @Test
    public void testRemoveItem() {
        final Criteria c = where("firstName").regex(".*d.*");
        final MongoContainer<ObjectId,Customer> mc =
                builder().forCriteria(c)
                .build();

        int initSize = mc.size();

        ObjectId itemId = mc.firstItemId();
        mc.removeItem(itemId);

        assertNotEquals(itemId, mc.firstItemId());
        assertNotEquals(initSize, mc.size());
    }

    @Test
    public void testRemoveAllItems() {
        final Criteria c = where("firstName").regex(".*d.*");
        final MongoContainer<ObjectId,Customer> mc =
                builder().forCriteria(c)
                        .build();

        mc.removeAllItems();

        assertEquals(0, mc.size());
    }

}
