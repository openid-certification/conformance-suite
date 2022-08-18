package net.openid.conformance.condition.client.jsonAsserting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

import static net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition.ROOT_PATH;

public class JsonKeysKeeper {

	private final List<String> jsonKeys = new ArrayList<>();
	private boolean hasNext;

	public void createListOfKeys(JsonElement element) {
		if (element.isJsonArray()) {
			this.jsonKeys.add("$");
		}

		if (element.isJsonObject()) {
			parseJsonObject(element.getAsJsonObject());
		} else if (element.isJsonArray()) {
			parseJsonArray(element.getAsJsonArray());
		}
	}

	public void remove(String name) {
		if (this.jsonKeys.contains(name)) {
			this.hasNext = this.jsonKeys.contains(name) && this.jsonKeys.size() > 1;
		}
		if (name.equals(ROOT_PATH) || name.equals("data")) {
			this.jsonKeys.remove("data");
		} else {
			this.jsonKeys.remove(name);
		}
	}

	public List<String> getFinalResult() {
		return this.jsonKeys;
	}

	public boolean hasNext() {
		return this.hasNext;
	}

	private void parseJsonObject(JsonObject jsonObject) {
		jsonObject.getAsJsonObject().keySet().forEach(key -> {
			this.jsonKeys.add(key);
			if (jsonObject.get(key).isJsonObject()) {
				parseJsonObject(jsonObject.get(key).getAsJsonObject());
			} else if (jsonObject.get(key).isJsonArray()) {
				parseJsonArray(jsonObject.get(key).getAsJsonArray());
			}
		});
	}

	private void parseJsonArray(JsonArray jsonArray) {
		jsonArray.forEach(element -> {
			if (element.isJsonObject()) {
				parseJsonObject(element.getAsJsonObject());
			} else if (element.isJsonArray()) {
				parseJsonArray(element.getAsJsonArray());
			}
		});
	}
}
