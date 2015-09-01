package org.tylproject.vaadin.addon.utils;

import com.vaadin.data.Container;
import com.vaadin.data.util.filter.*;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by evacchi on 02/12/14.
 */
public class DefaultFilterConverter implements FilterConverter {

    @Override
    public Criteria convert(Container.Filter f) {
        return convert(f, false);
    }

    @Override
    public Criteria convertNegated(Container.Filter filter) {
        return convert(filter, true);
    }

    @Override
    public List<Criteria> convertAll(Collection<Container.Filter> fs) {
        List<Criteria> cs = new ArrayList<Criteria>();
        for (Container.Filter f: fs)
            cs.add(convert(f));
        return cs;
    }

    public Criteria convert(Container.Filter f, boolean negated) {
        if (f instanceof IsNull) {
            return convertIsNullFilter((IsNull) f);
        } else
        if (f instanceof SimpleStringFilter) {
            return convertSimpleStringFilter(((SimpleStringFilter) f));
        } else
        if (f instanceof Compare) {
            return convertCompareFilter((Compare) f);
        } else
        if (f instanceof Not) {
            return convertNegated(((Not) f).getFilter());
        } else
        if (f instanceof And) {
            return convertAndFilter((And) f, negated);
        } else
        if (f instanceof Or) {
            return convertOrFilter((Or) f, negated);
        } else
        if (f instanceof Between) {
            return convertBetweenFilter((Between) f, negated);
        }

        throw new UnsupportedFilterException("Unsupported Filter "+f);
    }

    private Criteria convertBetweenFilter(Between btw, boolean negated) {
        Criteria c = Criteria.where(btw.getPropertyId().toString());
        if (negated) {
            if (btw.getStartValue() == null) {
                return c.gt(btw.getEndValue());
            } else if (btw.getEndValue() == null) {
                return c.lt(btw.getStartValue());
            } else
                return c.lt(btw.getStartValue())
                        .gt(btw.getEndValue());
        } else {
            if (btw.getStartValue() == null) {
                return c.lte(btw.getEndValue());
            } else if (btw.getEndValue() == null) {
                return c.gte(btw.getStartValue());
            } else
                return c.gte(btw.getStartValue())
                        .lte(btw.getEndValue());
        }
    }

    private Criteria convertCompareFilter(Compare f) {
        Compare ff = f;
        Criteria c = Criteria.where(ff.getPropertyId().toString());
        switch (ff.getOperation()) {
            case EQUAL:
                return c.is(ff.getValue());
            case GREATER:
                return c.gt(ff.getValue());
            case GREATER_OR_EQUAL:
                return c.gte(ff.getValue());
            case LESS:
                return c.lt(ff.getValue());
            case LESS_OR_EQUAL:
                return c.lte(ff.getValue());
            default:
                throw new IllegalArgumentException("Unknown comparison operator: "+ ff.getOperation());
        }
    }

    private Criteria convertIsNullFilter(IsNull f) {
        return Criteria.where(f.getPropertyId().toString()).is(null);
    }

    private Criteria convertOrFilter(Or f, boolean negated) {
        Collection<Container.Filter> filterList = f.getFilters();
        if (filterList.isEmpty()) {
            throw new IllegalStateException("filter list in Or is empty");
        }
        if (filterList.size() == 1) {
            return convert(filterList.iterator().next());
        }
        List<Criteria> cs = convertAll(filterList);
        if (negated) {
            return new Criteria().norOperator(cs.toArray(new Criteria[0]));
        } else {
            return new Criteria().orOperator(cs.toArray(new Criteria[0]));
        }
    }

    private Criteria convertAndFilter(And f, boolean negated) {
        if (negated)
            throw new UnsupportedFilterException("Not(And) not supported in " +
                    "mongo");

        Collection<Container.Filter> filterList = f.getFilters();
        if (filterList.isEmpty()) {
            throw new IllegalStateException("filter list in And is empty");
        }
        if (filterList.size() == 1) {
            return convert(filterList.iterator().next());
        }
        List<Criteria> cs = convertAll(filterList);
        return new Criteria().andOperator(cs.toArray(new Criteria[0]));
    }

    private Criteria convertSimpleStringFilter(SimpleStringFilter sf) {
        Criteria c = Criteria.where(sf.getPropertyId().toString());
        String filterString = Pattern.quote(sf.getFilterString()) + ".*";

        if (sf.isOnlyMatchPrefix()) {
            filterString = "^" + filterString;
        } else {
            filterString = ".*" + filterString;
        }
        if (sf.isIgnoreCase()) {
            return c.regex(filterString, "i");
        } else {
            return c.regex(filterString);
        }
    }



}
