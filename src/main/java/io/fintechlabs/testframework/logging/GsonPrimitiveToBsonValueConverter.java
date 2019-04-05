package io.fintechlabs.testframework.logging;

import org.bson.BsonBoolean;
import org.bson.BsonDouble;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

import com.google.gson.JsonPrimitive;

/**
 * Converts JsonPrimitives from GSON into BSON Values for Mongo.
 */
@Component
@WritingConverter
public class GsonPrimitiveToBsonValueConverter implements Converter<JsonPrimitive, BsonValue> {

	@Override
	public BsonValue convert(JsonPrimitive source) {
		if (source == null) {
			return null;
		} else if (source.isBoolean()) {
			return BsonBoolean.valueOf(source.getAsBoolean());
		} else if (source.isNumber()) {
			// TODO: should we have this optimize for integers, too?
			return new BsonDouble(source.getAsDouble());
		} else if (source.isString()) {
			return new BsonString(source.getAsString());
		} else {
			throw new IllegalArgumentException("Source JsonPrimitive not a known category: " + source);
		}

	}

}
