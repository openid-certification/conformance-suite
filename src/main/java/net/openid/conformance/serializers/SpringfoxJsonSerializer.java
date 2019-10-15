package net.openid.conformance.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import springfox.documentation.spring.web.json.Json;

import java.lang.reflect.Type;

public class SpringfoxJsonSerializer implements JsonSerializer<Json> {

	@Override
	public JsonElement serialize(Json json, Type type, JsonSerializationContext context) {
		JsonParser parser = new JsonParser();
		return parser.parse(json.value());
	}

}
