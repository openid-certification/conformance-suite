package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;
import java.util.TreeSet;

/**
 * RFC 8936 2.3 defines only {@code sets} and {@code moreAvailable} in a poll response. Other
 * members are not forbidden, but usually indicate a misspelled member on the sender side, so
 * callers grade this as a WARNING. Reads {@code ssf_polling_response}.
 */
public class OIDSSFWarnPollResponseUnknownMembers extends AbstractCondition {

	private static final Set<String> KNOWN_MEMBERS = Set.of("sets", "moreAvailable");

	@Override
	@PreEnvironment(required = "ssf_polling_response")
	public Environment evaluate(Environment env) {

		JsonElement bodyEl = env.getElementFromObject("ssf_polling_response", "body_json");
		if (bodyEl == null || !bodyEl.isJsonObject()) {
			log("The poll response body is not a JSON object; nothing to check for unknown members");
			return env;
		}

		Set<String> unknown = new TreeSet<>(bodyEl.getAsJsonObject().keySet());
		unknown.removeAll(KNOWN_MEMBERS);
		if (!unknown.isEmpty()) {
			throw error("The poll response contains members the poll response definition does not define",
				args("unknown_members", unknown, "known_members", KNOWN_MEMBERS));
		}

		logSuccess("The poll response contains only members the poll response definition defines");
		return env;
	}
}
