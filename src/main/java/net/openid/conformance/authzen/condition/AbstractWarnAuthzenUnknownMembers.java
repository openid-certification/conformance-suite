package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Flags members of a PDP response that the specification does not define. Receivers must ignore
 * unknown members (Section 10.1.1), so they are not forbidden, but in a response they usually mean
 * a misspelled or misplaced member; callers grade these conditions as a WARNING. Values that are
 * not objects are skipped, as their structure is checked by the response validation conditions.
 */
public abstract class AbstractWarnAuthzenUnknownMembers extends AbstractCondition {

	/**
	 * Records under {@code path} the members of {@code element} that are not in {@code known}.
	 */
	protected static void collectUnknownMembers(JsonElement element, String path, Set<String> known, Map<String, Set<String>> unknown) {
		if (element == null || !element.isJsonObject()) {
			return;
		}
		Set<String> names = new TreeSet<>(element.getAsJsonObject().keySet());
		names.removeAll(known);
		if (!names.isEmpty()) {
			unknown.put(path, names);
		}
	}

	/**
	 * Applies {@link #collectUnknownMembers} to each element of {@code array}, at {@code path[i]}.
	 */
	protected static void collectUnknownMembersOfEach(JsonElement array, String path, Set<String> known, Map<String, Set<String>> unknown) {
		if (array == null || !array.isJsonArray()) {
			return;
		}
		JsonArray elements = array.getAsJsonArray();
		for (int i = 0; i < elements.size(); i++) {
			collectUnknownMembers(elements.get(i), path + "[" + i + "]", known, unknown);
		}
	}

	protected static Map<String, Set<String>> newUnknownMembers() {
		return new LinkedHashMap<>();
	}

	protected Environment reportUnknownMembers(Environment env, Map<String, Set<String>> unknown, String what) {
		if (!unknown.isEmpty()) {
			throw error("The " + what + " contains members that the specification does not define",
				args("unknown_members", unknown));
		}
		logSuccess("The " + what + " contains only members that the specification defines");
		return env;
	}
}
