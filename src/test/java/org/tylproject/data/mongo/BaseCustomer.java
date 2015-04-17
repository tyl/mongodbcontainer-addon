package org.tylproject.data.mongo;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

/**
 * Created by marco on 17/04/15.
 */
public class BaseCustomer {

    @Id
    private ObjectId id;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
}
