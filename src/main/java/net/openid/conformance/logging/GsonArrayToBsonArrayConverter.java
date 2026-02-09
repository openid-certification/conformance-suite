package net.openid.conformance.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import org.bson.BsonArray;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GsonArrayToBsonArrayConverter implements Converter<JsonArray, BsonArray> {

	private Gson gson = new GsonBuilder().serializeNulls().create();

	@Override
	public BsonArray convert(JsonArray source) {
		if (source == null) {
			return null;
		} else {
			String json = gson.toJson(GsonObjectToBsonDocumentConverter.convertFieldsToStructure(source));
			return BsonArray.parse(json);
		}
	}

	/**
	 * Convert nimbus JOSE objects to their JSON representation so that Gson/Mongo
	 * can serialize them without reflective access to java.security internals.
	 */
	private static Object convertValue(Object value) {
		if (value instanceof JsonElement element && element.isJsonArray()) {
			return new GsonArrayToBsonArrayConverter().convert(element.getAsJsonArray());
		} else if (value instanceof JWK jwk) {
			return JsonParser.parseString(jwk.toJSONString());
		} else if (value instanceof JWKSet set) {
			return JsonParser.parseString(set.toString());
		} else if (value instanceof JWTClaimsSet set) {
			return JsonParser.parseString(set.toString());
		} else if (value instanceof JWSHeader header) {
			return JsonParser.parseString(header.toString());
		} else if (value instanceof List<?> list) {
			JsonArray arr = new JsonArray();
			for (Object item : list) {
				Object converted = convertValue(item);
				if (converted instanceof JsonElement el) {
					arr.add(el);
				} else if (converted != null) {
					arr.add(converted.toString());
				}
			}
			return arr;
		}
		return value;
	}

	public static Map<String, Object> convertUnloggableValuesInMap(Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		Map<String, Object> convertedMap = new HashMap<>();
		map.forEach((key, value) -> convertedMap.put(key, convertValue(value)));
		return convertedMap;
	}

}
