package org.tylproject.vaadin.addon.utils;

import com.vaadin.data.util.converter.Converter;
import org.bson.types.ObjectId;

import java.util.Locale;

/**
 * Created by evacchi on 19/01/15.
 */
public class StringToObjectIdConverter implements Converter<String, ObjectId> {
    @Override
    public ObjectId convertToModel(String value, Class<? extends ObjectId>
            targetType, Locale locale) throws ConversionException {
        return value == null || value.equals("") ?
                null
                : new ObjectId(value);
    }

    @Override
    public String convertToPresentation(ObjectId value, Class<? extends String>
            targetType, Locale locale) throws ConversionException {
        return value == null ? null : value.toString();
    }

    @Override
    public Class<ObjectId> getModelType() {
        return ObjectId.class;
    }

    @Override
    public Class<String> getPresentationType() {
        return String.class;
    }
}
