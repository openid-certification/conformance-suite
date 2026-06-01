package net.openid.conformance.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

/**
 * Registered Mongo custom converter for Gson {@link JsonObject} values. Delegates the
 * problematic-key wrapping (the {@code __wrapped_key_element_*} envelope) to
 * {@link MongoKeyWrapper#wrap}; see that class for the wrap/unwrap contract.
 */
@Component
@WritingConverter
public class GsonObjectToBsonDocumentConverter implements Converter<JsonObject, Bson> {

	private final Gson gson = new GsonBuilder().serializeNulls().create();

	@Override
	public Bson convert(JsonObject source) {
		if (source == null) {
			return null;
		}
		String json = gson.toJson(MongoKeyWrapper.wrap(source));
		return BsonDocument.parse(json);
	}
}
