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

	public JsonObjectBuilder addFields(String path, Map<String, String> values) {
		addFields(jsonObject, path, values);
		return this;
	}

	public JsonObject build() {
		return jsonObject;
	}

	public static void addField(JsonObject object, String path, String value) {
		Iterable<String> parts = Splitter.on('.').split(path);
		Iterator<String> it = parts.iterator();

		while(it.hasNext()) {
			String identifier = it.next();
			if(!it.hasNext()) {
				object.addProperty(identifier, value);
			} else {
				object = getOrCreate(object, identifier);
			}
		}
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
