package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;
import java.util.TreeSet;

/**
 * SSF 1.0 8.1.2.1 defines {@code stream_id}, {@code status} and {@code reason} in a stream
 * status document. Other members usually indicate a misspelled member on the sender side, so
 * callers grade this as a WARNING. Expects the response under {@code endpoint_response}.
 */
public class OIDSSFWarnStreamStatusResponseUnknownMembers extends AbstractCondition {

	private static final Set<String> KNOWN_MEMBERS = Set.of("stream_id", "status", "reason");

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement bodyEl = env.getElementFromObject("endpoint_response", "body_json");
		if (bodyEl == null || !bodyEl.isJsonObject()) {
			log("The stream status response is not a JSON object; nothing to check for unknown members");
			return env;
		}

		Set<String> unknown = new TreeSet<>(bodyEl.getAsJsonObject().keySet());
		unknown.removeAll(KNOWN_MEMBERS);
		if (!unknown.isEmpty()) {
			throw error("The stream status response contains members the stream status definition does not define",
				args("unknown_members", unknown, "known_members", KNOWN_MEMBERS));
		}

		logSuccess("The stream status response contains only members the stream status definition defines");
		return env;
	}
}
