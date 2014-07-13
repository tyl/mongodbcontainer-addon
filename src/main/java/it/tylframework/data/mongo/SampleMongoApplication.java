/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.tylframework.data.mongo;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import it.tylframework.addon.MongoDbQuery;
import it.tylframework.addon.MongoDbQueryDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class SampleMongoApplication implements CommandLineRunner {

	@Autowired
	private CustomerRepository repository;

	@Override
	public void run(String... args) throws Exception {

		repository.deleteAll();

		// save a couple of customers
		repository.save(new Customer("Andrea", "Novara"));
		repository.save(new Customer("Edoardo", "Vacchi"));
        repository.save(new Customer("Marco", "Pancotti"));
        repository.save(new Customer("Alessandro", "Mongelli"));
        repository.save(new Customer("Michele", "Sciabarra"));
        repository.save(new Customer("Adriano", "Marchetti"));
        repository.save(new Customer("Luca", "Buraggi"));

		// fetch all customers
		System.out.println("Customers found with findAll():");
		System.out.println("-------------------------------");
		for (Customer customer : repository.findAll()) {
			System.out.println(customer);
		}
		System.out.println();

    }

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleMongoApplication.class, args);
	}
}
