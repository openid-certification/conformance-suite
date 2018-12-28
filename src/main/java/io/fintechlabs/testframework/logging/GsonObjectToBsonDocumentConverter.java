package io.fintechlabs.testframework.logging;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.util.JSON;

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
 * @author jricher
 *
 */
@Component
@WritingConverter
public class GsonObjectToBsonDocumentConverter implements Converter<JsonObject, Bson> {

	private static final Logger log = LoggerFactory.getLogger(GsonObjectToBsonDocumentConverter.class);

	private Gson gson = new GsonBuilder().create();

	@Override
	public Bson convert(JsonObject source) {
		if (source == null) {
			return null;
		} else {
			return (Bson) JSON.parse(gson.toJson(convertFieldsToStructure(source)));
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
					converted.add("__wrapped_key_element_" + RandomStringUtils.randomAlphabetic(6), wrap);
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
		} else {
			return source;
		}
	}

}
