package net.openid.conformance.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

/**
 * Convert JsonObjects from GSON into a BSON Document, wrapping problematic keys with a conversion as follows:
 *
 *    "a.b": "foo"
 *
 *  becomes:
 *
 *    "__wrapped_key_element": {
 *      "key": "a.b",
 *      "value": "foo"
 *    }
 *
 */
@Component
@WritingConverter
public class GsonObjectToBsonDocumentConverter implements Converter<JsonObject, Bson> {
	public static final String CONFORMANCE_SUITE_JSON_NULL_CONSTANT = "CONFORMANCE_SUITE_JSON_NULL";
	private static final Logger log = LoggerFactory.getLogger(GsonObjectToBsonDocumentConverter.class);

	private Gson gson = new GsonBuilder().serializeNulls().create();

	@Override
	public Bson convert(JsonObject source) {
		if (source == null) {
			return null;
		} else {
			String json = gson.toJson(convertFieldsToStructure(source));
			return BsonDocument.parse(json);
		}
	}

	/**
	 * @param source
	 * @return
	 */
	public static JsonElement convertFieldsToStructure(JsonElement source) {
		if (source.isJsonObject()) {
			// need to look through all the fields and convert any weird ones
			JsonObject converted = new JsonObject();
			for (String key : source.getAsJsonObject().keySet()) {
				if (key.contains(".") || key.contains("$") || key.startsWith("__wrapped_key_element_")) {
					JsonObject wrap = new JsonObject();
					wrap.addProperty("key", key);
					wrap.add("value", convertFieldsToStructure(source.getAsJsonObject().get(key)));
					converted.add("__wrapped_key_element_" + RandomStringUtils.secure().nextAlphabetic(6), wrap);
					log.info("Wrapped " + key + " as " + wrap.toString());
				} else {
					converted.add(key, convertFieldsToStructure(source.getAsJsonObject().get(key)));
				}
			}
			return converted;
		} else if (source.isJsonArray()) {
			JsonArray converted = new JsonArray();
			for (JsonElement element : source.getAsJsonArray()) {
				converted.add(convertFieldsToStructure(element));
			}
			return converted;
		} else if(source.isJsonNull()) {
			return new JsonPrimitive(CONFORMANCE_SUITE_JSON_NULL_CONSTANT);
		} else {
			return source;
		}
	}

}
