package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flags members of a PDP response whose value is null, at any depth. Section 11.5 says properties
 * with a value of null SHOULD be omitted from JSON objects; callers grade these conditions as a
 * WARNING. The response validation conditions treat a null OPTIONAL member as absent and fail a
 * null REQUIRED one, so this only reports the null.
 */
public abstract class AbstractWarnAuthzenNullMembers extends AbstractCondition {

	/**
	 * Records the path of each object member under {@code element} whose value is null.
	 */
	protected static void collectNullMembers(JsonElement element, String path, List<String> nullMembers) {
		if (element == null) {
			return;
		}
		if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			for (Map.Entry<String, JsonElement> member : object.entrySet()) {
				String memberPath = path + "." + member.getKey();
				if (member.getValue().isJsonNull()) {
					nullMembers.add(memberPath);
				} else {
					collectNullMembers(member.getValue(), memberPath, nullMembers);
				}
			}
		} else if (element.isJsonArray()) {
			JsonArray array = element.getAsJsonArray();
			for (int i = 0; i < array.size(); i++) {
				collectNullMembers(array.get(i), path + "[" + i + "]", nullMembers);
			}
		}
	}

	protected Environment reportNullMembers(Environment env, String envKey, String what) {
		List<String> nullMembers = new ArrayList<>();
		collectNullMembers(env.getObject(envKey), "$", nullMembers);
		if (!nullMembers.isEmpty()) {
			throw error("The " + what + " contains members whose value is null; a member without a value should be omitted",
				args("null_members", nullMembers, "response", env.getObject(envKey)));
		}
		logSuccess("The " + what + " contains no members whose value is null");
		return env;
	}
}
