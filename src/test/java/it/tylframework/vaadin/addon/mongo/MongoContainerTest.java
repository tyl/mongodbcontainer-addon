package it.tylframework.vaadin.addon.mongo;

import com.mongodb.MongoClient;
import com.vaadin.data.util.BeanItem;
import it.tylframework.data.mongo.Customer;
import it.tylframework.vaadin.addon.MongoContainer;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

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
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = SampleMongoApplication.class)

public class MongoContainerTest {
    private final MongoOperations mongoOps;
    private final Class<Customer> beanClass = Customer.class;

    public MongoContainerTest() throws Exception {
        this.mongoOps = new MongoTemplate(new MongoClient(), "database");
    }

    public MongoContainer.Builder builder() {
        return MongoContainer.Builder.with(mongoOps)
                .withBeanClass(beanClass)
                .withPageSize(3);
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
    public void testProperties() {
        System.out.println("propids"+builder().build().getContainerPropertyIds());
        assertNotEquals(0, builder().build().getContainerPropertyIds());
    }



    @Test
    public void testSize(){
        assertEquals(builder().build().size(), 7);
    }

    @Test
    public void firstItem() {
        MongoContainer<Customer> mc = builder().build();
        ObjectId firstId = mc.firstItemId();
        BeanItem<Customer> item = mc.getItem(firstId);
        Customer c = item.getBean();
        assertEquals("Andrea", c.getFirstName());
        assertTrue(mc.isFirstId(firstId));
        assertFalse(mc.isLastId(firstId));
    }

    @Test
    public void lastItem() {
        MongoContainer<Customer> mc = builder().build();
        ObjectId lastId = mc.lastItemId();
        BeanItem<Customer> item = mc.getItem(lastId);
        Customer c = item.getBean();
        assertEquals("Luca", c.getFirstName());
        assertFalse(mc.isFirstId(lastId));
        assertTrue(mc.isLastId(lastId));
    }

    @Test
    public void testIndexOf() {
        final MongoContainer<Customer> mc = builder().build();

        ObjectId id = mc.getIdByIndex(5);
        Customer ev = mongoOps.findOne(query(where("firstName").is("Adriano")), Customer.class);
                System.out.println(ev);
                assertEquals(ev.getId(), id);

    }

    // fetch using Container
    @Test
    public void testLoadItems() {

        final Criteria crit = where("firstName").regex(".*d.*");

        final MongoContainer<Customer> mc =
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
        final MongoContainer<Customer> mc =
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
        final MongoContainer<Customer> mc =
                builder().forCriteria(c)
                        .build();

        mc.removeAllItems();

        assertEquals(0, mc.size());
    }

    @Test
    public void testAddItem() {
        final MongoContainer<Customer> mc = builder().build();
        final Customer apancotti = new Customer("Andrea", "Pancotti");
        mc.addEntity(apancotti);
        assertFalse(
            mongoOps.find(
                Query.query(
                   where("firstName").is("Andrea")
                    .and("lastName") .is("Pancotti")), Customer.class).isEmpty()
        );
    }

    @Test
    public void testRange() {
        final MongoContainer<Customer> mc = builder().build();
        assertEquals(1, mc.getItemIds(0, 1).size());
        assertEquals(1, mc.getItemIds(mc.size() - 1, 1).size());
        assertEquals(3, mc.getItemIds(3, 3).size());

    }

}
