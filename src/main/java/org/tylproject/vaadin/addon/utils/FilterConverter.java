package org.tylproject.vaadin.addon.utils;

import com.vaadin.data.Container;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collection;
import java.util.List;

/**
 * Created by evacchi on 02/12/14.
 */
public interface FilterConverter {
    Criteria convert(Container.Filter f);

    Criteria convertNegated(Container.Filter filter);

    List<Criteria> convertAll(Collection<Container.Filter> fs);
}
