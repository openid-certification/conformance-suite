package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class OIDSSFExtractVerificationEventFromReceivedSETs extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf_polling_response")
	public Environment evaluate(Environment env) {

		JsonObject ssfPollingResponse = env.getObject("ssf_polling_response");
		JsonObject bodyJson = ssfPollingResponse.getAsJsonObject("body_json");
		if (bodyJson == null) {
			throw error("Missing json body in polling response", args("polling_response", ssfPollingResponse));
		}

		JsonObject setsObject = bodyJson.getAsJsonObject("sets");

		var setEntry = setsObject.entrySet().stream().findFirst().orElse(null);
		if (setEntry == null) {
			throw error("Could not find verification event in empty polling response", args("polling_response", ssfPollingResponse));
		}

		// String setKey = setEntry.getKey();
		String setJwt = OIDFJSON.getString(setEntry.getValue());

		JsonObject eventsClaim;
		try {
			JWT jwt = JWTUtil.parseJWT(setJwt);
			JsonObject claimsObject = JWTUtil.jwtClaimsSetAsJsonObject(jwt);
			eventsClaim = claimsObject.getAsJsonObject("events");
		} catch (ParseException e) {
			throw error("Couldn't parse SET JWT", e);
		}

		if (eventsClaim == null || !eventsClaim.has("https://schemas.openid.net/secevent/ssf/event-type/verification")) {
			throw error("Couldn't find verification event in polling response", args("events", eventsClaim, "polling_response", ssfPollingResponse));
		}

		env.putString("ssf", "verification.jwt", setJwt);
		logSuccess("Found verification event in polling response", args("events_claim", eventsClaim, "polling_response", ssfPollingResponse));
		return env;
	}
}
