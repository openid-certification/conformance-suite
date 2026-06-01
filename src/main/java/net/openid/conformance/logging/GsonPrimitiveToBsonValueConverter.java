package net.openid.conformance.logging;

import com.google.gson.JsonPrimitive;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bson.BsonBoolean;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
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
			// Preserve integer-ness: JSON "5" must round-trip as BsonInt32/BsonInt64, not BsonDouble.
			// Mirrors the LazilyParsedNumber branch in GsonArrayToBsonArrayConverter.convertValue.
			String text = OIDFJSON.getNumber(source).toString();
			try {
				long l = Long.parseLong(text);
				if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
					return new BsonInt32((int) l);
				}
				return new BsonInt64(l);
			} catch (NumberFormatException ignored) {
				return new BsonDouble(Double.parseDouble(text));
			}
		} else if (source.isString()) {
			return new BsonString(OIDFJSON.getString(source));
		} else {
			throw new IllegalArgumentException("Source JsonPrimitive not a known category: " + source);
		}

	}

}
