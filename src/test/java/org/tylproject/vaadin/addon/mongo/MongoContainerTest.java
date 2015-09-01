package org.tylproject.vaadin.addon.mongo;

import com.mongodb.MongoClient;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.Not;
import com.vaadin.data.util.filter.Or;
import com.vaadin.data.util.filter.SimpleStringFilter;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.springframework.data.domain.Sort;
import org.tylproject.data.mongo.Customer;
import org.tylproject.data.mongo.Person;
import org.tylproject.vaadin.addon.MongoContainer;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Created by evacchi on 26/09/14.
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = SampleMongoApplication.class)

public class MongoContainerTest extends BaseTest {
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
        assertEquals("Austin", c.getFirstName());
        assertTrue(mc.isFirstId(firstId));
        assertFalse(mc.isLastId(firstId));
    }

    @Test
    public void lastItem() {
        MongoContainer<Customer> mc = builder().build();
        ObjectId lastId = mc.lastItemId();
        BeanItem<Customer> item = mc.getItem(lastId);
        Customer c = item.getBean();
        assertEquals("Susan", c.getFirstName());
        assertFalse(mc.isFirstId(lastId));
        assertTrue(mc.isLastId(lastId));
    }

    @Test
    public void testIndexOf() {
        final MongoContainer<Customer> mc = builder().build();

        ObjectId id = mc.getIdByIndex(5);
        Customer ev = mongoOps.findOne(query(where("firstName").is("Keith")), Customer.class);
                System.out.println(ev);
                assertEquals(ev.getId(), id);

    }


    /**
     * Version 0.9.5 raised an exception
     * when containsId() was called twice
     */
    @Test
    public void testContainsId() {
        MongoContainer<Customer> mc = builder().build();
        ObjectId firstId = mc.firstItemId();

        mc.containsId(firstId);
        mc.containsId(firstId);


    }

    @Test
    public void testSort() {
        final MongoContainer<Customer> mc =
                builder().build();

        final Object[] columns = { "lastName" };
        final boolean[] ascending = { false };

        mc.sort(columns, ascending);

        Object id1 = mc.firstItemId();
        assertEquals("Scott", mc.getItem(id1).getBean().getLastName());

        for (int i = 0; i < mc.size(); i++) {
            Object id = mc.getIdByIndex(i+1);
            id1 = mc.nextItemId(id1);
            assertEquals(id1, id);
        }

        assertEquals(mc.getItem(mc.lastItemId()).getBean().getLastName(), "Long");

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
    public void testAddOnEmpty() {
        removeAll();
        
        final MongoContainer<Customer> mc = 
                MongoContainer.Builder.forEntity(beanClass,mongoOps).build();

        final Customer ljenkins = new Customer("Leroy", "Jenkins");
        mc.addEntity(ljenkins);
        assertFalse(
            mongoOps.find(
                Query.query(
                   where("firstName").is("Leroy")
                    .and("lastName") .is("Jenkins")), Customer.class).isEmpty()
        );

    }

    @Test
    public void testAddItem() {
        final MongoContainer<Customer> mc = builder().build();
        final Customer ljenkins = new Customer("Leroy", "Jenkins");
        mc.addEntity(ljenkins);
        assertFalse(
            mongoOps.find(
                Query.query(
                   where("firstName").is("Leroy")
                    .and("lastName") .is("Jenkins")), Customer.class).isEmpty()
        );
    }

    @Test
    public void testGetItemIds() {
        final MongoContainer<Customer> mc = builder().build();
        assertEquals(mc.size(), mc.getItemIds().size());
    }

    @Test
    public void testRange() {
        final MongoContainer<Customer> mc = builder().build();
        assertEquals(1, mc.getItemIds(0, 1).size());
        assertEquals(1, mc.getItemIds(mc.size() - 1, 1).size());
        assertEquals(3, mc.getItemIds(3, 3).size());

    }

}
