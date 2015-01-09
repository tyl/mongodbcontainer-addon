package org.tylproject.vaadin.addon.mongo;

import com.mongodb.MongoClient;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.Not;
import com.vaadin.data.util.filter.Or;
import com.vaadin.data.util.filter.SimpleStringFilter;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.tylproject.data.mongo.Customer;
import org.tylproject.vaadin.addon.MongoContainer;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Created by evacchi on 09/01/15.
 */

public class FilterTest extends BaseTest {

    // fetch using Container
    @Test
    public void testContainsFilteredItem() {


        final MongoContainer<Customer> mc =
                builder().build();

        assertEquals(7, mc.size());


        mc.addContainerFilter(new SimpleStringFilter("firstName", "i", false, false));

        List<Customer> bs = mongoOps.find(query(where("firstName").regex(".*i.*")), beanClass);


        assertEquals(5, mc.size());


        for (Customer c: bs) {
            System.out.println(c);
            assertEquals(true, mc.containsId(c.getId()));
        }


        mc.removeAllContainerFilters();

        assertEquals(7, mc.size());
    }

    @Test
    public void testFilterList() {
        final Criteria crit = where("firstName").regex(".*i.*");

        final MongoContainer<Customer> mc =
                builder().build();

        assertEquals(7, mc.size());

        List<Customer> bs = mongoOps.find(query(where("firstName").regex(".*i.*")), beanClass);

        mc.addContainerFilter(new SimpleStringFilter("firstName", "i", false, false));

        ObjectId itemId = mc.firstItemId();
        int i = 0;
        do {
            BeanItem<Customer> item = mc.getItem(itemId);
            assertEquals(bs.get(i++), item.getBean());
            itemId = mc.nextItemId(itemId);
        } while (itemId != null);

    }

    @Test
    public void testOrFilter() {

        final MongoContainer<Customer> mc =
                builder().build();

        mc.addContainerFilter(new Or(
                new SimpleStringFilter("firstName", "i", false, false),
                new SimpleStringFilter("firstName", "x", false, false)
        ));

        ObjectId itemId = mc.firstItemId();

        // should not raise error
    }

    @Test
    public void testNOrFilter() {

        final MongoContainer<Customer> mc =
                builder().build();

        mc.addContainerFilter(new Not(new Or(
                new SimpleStringFilter("firstName", "i", false, false),
                new SimpleStringFilter("firstName", "x", false, false)
        )));

        ObjectId itemId = mc.firstItemId();

        // should not raise error
    }

    @Test
    public void testAndFilter() {

        final MongoContainer<Customer> mc =
                builder().build();

        mc.addContainerFilter(new And(
                new SimpleStringFilter("firstName", "i", false, false),
                new SimpleStringFilter("firstName", "x", false, false)
        ));

        ObjectId itemId = mc.firstItemId();

        // should not raise error
    }


}
