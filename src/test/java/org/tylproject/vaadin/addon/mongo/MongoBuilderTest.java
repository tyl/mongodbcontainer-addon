package org.tylproject.vaadin.addon.mongo;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.tylproject.data.mongo.Person;
import org.tylproject.vaadin.addon.MongoContainer;

import static org.junit.Assert.assertEquals;

/**
 * Created by evacchi on 25/02/15.
 */
public class MongoBuilderTest {

    public MongoOperations makeMongoOps() {
        try {
            return new MongoTemplate(new MongoClient(), "test");
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @Test
    public void testProperties() {
        MongoContainer<Person> ps =
                MongoContainer.Builder.forEntity(Person.class, makeMongoOps())
                    .withProperty("name").build();

        assertEquals(1, ps.getContainerPropertyIds().size());
        assertEquals(String.class, ps.getType("name"));

    }

    @Test
    public void testNestedProperties() {
        MongoContainer<Person> ps =
            MongoContainer.Builder.forEntity(Person.class, makeMongoOps()).build();

        assertEquals(2, ps.getContainerPropertyIds().size());


        MongoContainer<Person> psNested =
            MongoContainer.Builder.forEntity(Person.class, makeMongoOps())
                    .withNestedProperty("address.street", String.class).build();

        assertEquals(3, psNested.getContainerPropertyIds().size());

    }



}
