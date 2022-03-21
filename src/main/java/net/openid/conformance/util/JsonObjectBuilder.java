package net.openid.conformance.util;

import com.google.common.base.Splitter;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonObjectBuilder {

	private JsonObject jsonObject = new JsonObject();

	public JsonObjectBuilder addField(String path, String value) {
		addField(jsonObject, path, value);
		return this;
	}

	public JsonObjectBuilder addField(String path, int value) {
		addField(jsonObject, path, value);
		return this;
	}

	public JsonObjectBuilder addFields(String path, Map<String, String> values) {
		addFields(jsonObject, path, values);
		return this;
	}

	public JsonObject build() {
		return jsonObject;
	}

	public static void addField(JsonObject object, String path, Object value) {
		Iterable<String> parts = Splitter.on('.').split(path);
		Iterator<String> it = parts.iterator();

		while(it.hasNext()) {
			String identifier = it.next();
			if(!it.hasNext()) {
				if(value instanceof Number) {
					addProperty(object, identifier,(Number) value);
					continue;
				}
				if(value instanceof Boolean) {
					addProperty(object, identifier,(Boolean) value);
					continue;
				}
				if(value instanceof String) {
					addProperty(object, identifier,(String) value);
					continue;
				}
				if(value instanceof Character) {
					addProperty(object, identifier,(Character) value);
					continue;
				}
				throw new RuntimeException("Unable to add property of type " + value.getClass());

			} else {
				object = getOrCreate(object, identifier);
			}
		}
	}

	private static void addProperty(JsonObject object, String identifier, String value) {
		object.addProperty(identifier, value);
	}

	private static void addProperty(JsonObject object, String identifier, Number value) {
		object.addProperty(identifier, value);
	}

	private static void addProperty(JsonObject object, String identifier, Boolean value) {
		object.addProperty(identifier, value);
	}

	private static void addProperty(JsonObject object, String identifier, Character value) {
		object.addProperty(identifier, value);
	}

	public static void addFields(JsonObject object, String path, Map<String, String> values) {

		List<String> parts = Splitter.on('.').splitToList(path);

		for(String part: parts) {
			object = getOrCreate(object, part);
		}

		for(Map.Entry<String, String> value: values.entrySet()) {
			object.addProperty(value.getKey(), value.getValue());
		}
	}

	public static JsonObject getOrCreate(JsonObject parent, String identifier) {
		if(parent.has(identifier)) {
			return parent.getAsJsonObject(identifier);
		}
		JsonObject newObject = new JsonObject();
		parent.add(identifier, newObject);
		return newObject;
	}

}
