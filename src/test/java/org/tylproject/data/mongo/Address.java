package org.tylproject.data.mongo;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

/**
 * Created by evacchi on 26/09/14.
 */

public class Address {
    @Id
    ObjectId id = new ObjectId();
    String text;

    public Address(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Address{" +
                "text='" + text + '\'' +
                '}';
    }
}
