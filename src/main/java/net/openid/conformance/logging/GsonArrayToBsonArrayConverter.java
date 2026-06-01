package net.openid.conformance.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.LazilyParsedNumber;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.springframework.core.convert.converter.Converter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GsonArrayToBsonArrayConverter implements Converter<JsonArray, BsonArray> {

	private static final Gson GSON = new GsonBuilder().serializeNulls().create();

	@Override
	public BsonArray convert(JsonArray source) {
		if (source == null) {
			return null;
		} else {
			String json = GSON.toJson(MongoKeyWrapper.wrap(source));
			return BsonArray.parse(json);
		}
	}

	/**
	 * Convert nimbus JOSE objects to their JSON representation so that Gson/Mongo
	 * can serialize them without reflective access to java.security internals.
	 */
	private static Object convertValue(Object value) {
		if (value instanceof Map<?, ?> mapValue) {
			// Nimbus claim sets (JWTClaimsSet.toJSONObject()) and other parsed JSON objects arrive
			// as a plain java.util.Map (e.g. net.minidev JSONObject), not a Gson JsonObject. Gson
			// JsonObject/JsonArray values are handled by Mongo custom converters, but plain maps are
			// walked directly by MappingMongoConverter. Wrap and encode maps before that happens.
			// Without this an args("payload", claimSet) log
			// of e.g. credential_configurations_supported.eu.europa.ec.eudi.pid.1 crashes the log write.
			JsonElement wrapped = MongoKeyWrapper.wrap(GSON.toJsonTree(mapValue));
			return BsonDocument.parse(GSON.toJson(wrapped));
		} else if (value instanceof JWK jwk) {
			return JsonParser.parseString(jwk.toJSONString());
		} else if (value instanceof JWKSet set) {
			return JsonParser.parseString(set.toString());
		} else if (value instanceof JWTClaimsSet set) {
			return JsonParser.parseString(set.toString());
		} else if (value instanceof JWSHeader header) {
			return JsonParser.parseString(header.toString());
		} else if (value instanceof LazilyParsedNumber lpn) {
			// BSON has no codec for Gson's lazy-parsed number; coerce to a standard Java number.
			try {
				return Long.parseLong(lpn.toString());
			} catch (NumberFormatException ignored) {
				return Double.parseDouble(lpn.toString());
			}
		}
		else if (value instanceof List<?> list) {
			JsonArray arr = new JsonArray();
			for (Object item : list) {
				Object converted = item instanceof Map<?, ?> mapItem
					? GSON.toJsonTree(mapItem)
					: convertValue(item);
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
		Map<String, Object> convertedMap = new LinkedHashMap<>();
		map.forEach((key, value) -> convertedMap.put(key, convertValue(value)));
		return convertedMap;
	}

}
