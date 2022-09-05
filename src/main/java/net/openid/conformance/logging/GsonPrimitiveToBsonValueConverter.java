package net.openid.conformance.logging;

import com.google.gson.JsonPrimitive;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bson.BsonBoolean;
import org.bson.BsonDouble;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

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
			return BsonBoolean.valueOf(OIDFJSON.getBoolean(source));
		} else if (source.isNumber()) {
			// TODO: should we have this optimize for integers, too?
			return new BsonDouble(OIDFJSON.getDouble(source));
		} else if (source.isString()) {
			return new BsonString(OIDFJSON.getString(source));
		} else {
			throw new IllegalArgumentException("Source JsonPrimitive not a known category: " + source);
		}

	}

}
