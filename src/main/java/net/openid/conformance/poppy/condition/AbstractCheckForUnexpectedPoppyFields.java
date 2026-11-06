package net.openid.conformance.poppy.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;

import java.util.List;
import java.util.Set;

abstract class AbstractCheckForUnexpectedPoppyFields extends AbstractCondition {

	protected static void checkObject(List<String> unexpected, String path, JsonElement element, Set<String> known) {
		if (element == null || !element.isJsonObject()) {
			return;
		}
		for (String key : element.getAsJsonObject().keySet()) {
			if (!known.contains(key)) {
				unexpected.add(path.isEmpty() ? key : path + "." + key);
			}
		}
	}

	protected static void checkArrayEntries(List<String> unexpected, String path, JsonElement element, Set<String> known) {
		if (element == null || !element.isJsonArray()) {
			return;
		}
		JsonArray array = element.getAsJsonArray();
		for (int i = 0; i < array.size(); i++) {
			checkObject(unexpected, path + "[" + i + "]", array.get(i), known);
		}
	}

	protected static JsonElement child(JsonObject json, String name) {
		return json.get(name);
	}
}
