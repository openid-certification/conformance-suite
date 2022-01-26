package net.openid.conformance.raidiam.validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public class Utils {

	/**
	 * @param fieldName Use "", if map is not inner field;
	 */
	public static void convertJsonMapToJsonArray(JsonElement body, String fieldName) {
		JsonArray array = new JsonArray();
		JsonObject temp = null;
		if (fieldName.equals("")) {
			temp = body.getAsJsonObject();
			fieldName = "data";
		} else {
			temp =  body.getAsJsonObject().get(fieldName).getAsJsonObject();
		}
		for (Map.Entry<String, JsonElement> entry : temp.entrySet()) {
			array.add(entry.getValue());
		}
		body.getAsJsonObject().add(fieldName, array);
	}
}
