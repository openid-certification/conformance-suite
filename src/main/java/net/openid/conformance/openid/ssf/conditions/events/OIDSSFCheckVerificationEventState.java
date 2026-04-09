package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCheckVerificationEventState extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonObject setClaimsJsonObject = env.getElementFromObject("ssf", "verification.token.claims").getAsJsonObject();

		JsonObject eventsObject = setClaimsJsonObject.getAsJsonObject("events");
		String verificationEventKey = SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE;
		JsonElement eventsEL = eventsObject.get(verificationEventKey);
		if (eventsEL == null) {
			throw error("Expected to find verification events object", args("missing_key", verificationEventKey, "events_object", eventsObject));
		}

		String expectedVerificationState = env.getString("ssf", "verification.state");

		JsonObject verificationEventObject = eventsEL.getAsJsonObject();
		if (!verificationEventObject.has("state")) {
			throw error("Expected to find state in verification event",
				args("events_object", verificationEventObject, "expected_state", expectedVerificationState));
		}

		JsonElement state = verificationEventObject.get("state");
		String actualVerificationState = OIDFJSON.getString(state);
		if (!actualVerificationState.equals(expectedVerificationState)) {
			throw error("Verification state check failed due to state mismatch", args("expected_state", expectedVerificationState, "actual_state", actualVerificationState, "claims", setClaimsJsonObject));
		}

		logSuccess("Verified verification event state", args("expected_state", expectedVerificationState, "actual_state", actualVerificationState));

		return env;
	}

}
