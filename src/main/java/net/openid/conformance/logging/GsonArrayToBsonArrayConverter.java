package net.openid.conformance.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBList;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;
import java.util.Map;

public class GsonArrayToBsonArrayConverter implements Converter<JsonArray, BasicDBList> {

	private Gson gson = new GsonBuilder().create();

	/* (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("deprecation")
	public BasicDBList convert(JsonArray source) {
		if (source == null) {
			return null;
		} else {
			return (BasicDBList) com.mongodb.util.JSON.parse(gson.toJson(GsonObjectToBsonDocumentConverter.convertFieldsToStructure(source)));

		}
	}

	public static Map<String, Object> convertUnloggableValuesInMap(Map<String, Object> map) {
		Map<String, Object> convertedMap = new HashMap<>();
		if (map != null) {
			map.forEach((key, value) -> {
				if (value instanceof JsonElement && ((JsonElement) value).isJsonArray()) {
					convertedMap.put(key, new GsonArrayToBsonArrayConverter().convert(((JsonElement) value).getAsJsonArray()));
				} else if (value instanceof JWK) {
					// letting this through to the default mongo converter results in stackoverflows if the jwk
					// contains an x5c entry; explicitly convert it to it's more helpful JSON representation
					String json = ((JWK) value).toJSONString();
					convertedMap.put(key, new JsonParser().parse(json));
				} else {
					convertedMap.put(key, value);
				}
			});
			return convertedMap;
		}
		return null;
	}

}
