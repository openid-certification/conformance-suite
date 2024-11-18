package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCheckVerificationEventState extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject setClaimsJsonObject = env.getElementFromObject("ssf", "verification.token.claims").getAsJsonObject();

		JsonObject eventsObject = setClaimsJsonObject.getAsJsonObject("events");
		String verificationEventKey = "https://schemas.openid.net/secevent/ssf/event-type/verification";
		JsonElement eventsEL = eventsObject.get(verificationEventKey);
		if (eventsEL == null) {
			logFailure("Expected to find verification events object", args("missing_key", verificationEventKey, "events_object", eventsObject));
			return env;
		}

		JsonObject verificationEventObject = eventsEL.getAsJsonObject();

		String expectedVerificationState = env.getString("ssf", "verification.state");
		String actualVerificationState = OIDFJSON.getString(verificationEventObject.get("state"));
		if (!actualVerificationState.equals(expectedVerificationState)) {
			logFailure("Verification state check failed due to state mismatch", args("expected_state", expectedVerificationState, "actual_state", actualVerificationState, "claims", setClaimsJsonObject));
			return env;
		}

		logSuccess("Verified verification event state", args("expected_state", expectedVerificationState, "actual_state", actualVerificationState));

		return env;
	}

}
