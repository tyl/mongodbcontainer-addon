# Vaadin Lazy MongoContainer

A Lazy Vaadin Container for MongoDB based on Spring Data.

## Features

- Automatic Bean Mapping through Spring Data's `MongoTemplate` 
- Rich Query Interface through Spring Data's `Criteria` objects
- Optional `Buffered` variant for batch editing
- Lazy loading of new Beans to handle large datasets 


## Installing

With Maven, add this to your `pom.xml`

```xml
<dependency>
    <artifactId>mongodbcontainer-addon</artifactId>
    <groupId>org.tylproject.vaadin.addon.mongodbcontainer</groupId>
    <version>0.9.3-beta.RELEASE</version>
</dependency>
```

## Demo

Clone the Demo repository with: 

```bash
git clone https://github.com/tyl/mongodbcontainer-demo
```

and run using


```bash
mvn jetty:run
```


## Example Usages

Consider the `Person` entity 

```java
public class Person {
   @Id private ObjectId id;
   private String firstName;
   private String lastName; 
   private Address address;
}
public class Address {
    private String street;
    private String zipCode;
    private String city;
    private String state;
}
```

### Simple MongoContainer

A simple `MongoContainer` instance writes back any change immediately on the backing MongoDB storage. Changes are committed immediately, as you insert, edit or remove items. You can create a simple container using the fluent builder

```java
MongoOperations mo = new MongoTemplate(new MongoClient(), "database");
MongoContainer<Person> mongoContainer = 
		  MongoContainer.Builder
		    .forEntity(beanClass, mo)
		    .forCriteria(where("firstName").is("Paul")).build();
```

For the `Criteria` object, refer to [Spring's documentation](http://docs.spring.io/spring-data/mongodb/docs/current/reference/html/). Vaadin standard "Filters" are also supported since v0.9.3. In this case, the Criteria object is always kept even when removeAllFilters() is invoked.

`Items` are implemented using Vaadin's `BeanItem<T>` (where `T` in this case is `Person `), so you can access the underlying entity instance using `item.getBean()`.
The container can be bound it to a widget such as `Table` as usual:

```java
Table t = new Table("Person", mongoContainer);
```

#### Adding New Entities

Use the `addEntity(T)` method:

```java
mongoContainer.addEntity(new Person("Paul", "McCartney"));
```

### BufferedMongoContainer

A simple `MongoContainer` instance writes back any change immediately on the backing MongoDB storage. If you want to edit items by batches with the ability to commit or discard changes, before they are made available on the database, you may want to use a `BufferedMongoContainer`.

A `BufferedMongoContainer` augments the `MongoContainer` with the `com.vaadin.data.Buffered` interface; as such it provides the methods `commit()`, which commits new, updated and deleted items to Mongo and `discard()`, which discards any change.
A `BufferedMongoContainer` can be obtained through the `MongoContainer.Builder` using `buildBuffered()` at the end of the chain:

```java
BufferedMongoContainer<Person> mongoContainer = 
		  MongoContainer.Builder
		    .forEntity(beanClass, mo)
		    .forCriteria(where("firstName").is("Paul")).buildBuffered();
```

The *buffered* property is a build-time property, that cannot be changed after a container has been butil. In other words, the method
`Buffered.setBuffered()` in `BufferedMongoContainer` always throws an `UnsupportedOperationException()`:

```java
mongoContainer.setBuffered(false); // can't do that: but you can build() a non-buffered container
```

#### Adding New Entities

`addEntity(T)` is still available, but it is also possible to use the Vaadin-style `addItem()` API, which will return a new `ObjectId`.

```java
ObjectId itemId = mongoContainer.addItem();
BeanItem<Person> item = mongoContainer.getItem(itemId);
// display the item through a form UI
...
```
#### Notifying the Buffered Container of the Updates

A buffered container must be notified when you add or update elements using a specific method. This is because the internal state must be kept in sync with users modifications. 

```java
// display the item through a form UI
...
// then, on form close
mongoContainer.notifyItemUpdated(updatedItemId,updatedItemId)`.
```
NOTE: This is not necessary for non-buffered containers, as the items can be refreshed from the mongo storage directly (e.g., using `table.refreshRowCache()`); in fact, on non-buffered containers the method is unavailable.


## Design Choices 

Any `MongoContainer` instance is bound to the given (optional) Spring `Criteria`. As such, the container contents always reflect these constraints; therefore, the MongoContainer **does not** implement interfaces or methods that imply violating this rule. For instance:

- Data is loaded *lazily* into pages. In order to ensure a low memory usage, only the ObjectIds are kept, actual data is loaded on demand.
- Properties cannot be added or removed on the container; if you want to change the list of columns, do so at build-time.
- ~~`Container.Sortable` is not implemented.~~ A query, can be sorted at construction time using `Builder.sortedBy(Sort)`. Since v0.9.4 you can also sort the container through Vaadin's `sort()` method. 
- As seen above, it is not possible to change a `BufferedMongoContainer` to a non-buffered container using `.setBuffered()`
- ~~Filters are currently applied through the fluent `Criteria` and `Query` interfaces.~~ since v0.9.3 MongoContainer implements the `Container.Filterable` interface and supports all Vaadin's standard filters, as indicated by the Book of Vaadin. In order to support more filters you can easily extend the `DefaultFilterConverter` with more cases.


