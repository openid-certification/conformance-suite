package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import net.openid.conformance.testmodule.OIDFJSON;

public class ConnectIDValidateLoginHint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {
		JsonElement loginHintElement = env.getElementFromObject("backchannel_request_object", "claims.login_hint");
		if (loginHintElement == null) {
			throw error("login_hint is missing");
		}

		if (!loginHintElement.isJsonObject()) {
			throw error("login_hint must be a JSON object for ConnectID", args("login_hint", loginHintElement));
		}

		JsonObject loginHint = loginHintElement.getAsJsonObject();

		if (!loginHint.has("format")) {
			throw error("login_hint is missing required 'format' field", args("login_hint", loginHint));
		}

		String format = OIDFJSON.getString(loginHint.get("format"));
		if ("phone_number".equals(format)) {
			if (!loginHint.has("phone_number")) {
				throw error("login_hint with format 'phone_number' is missing 'phone_number' field", args("login_hint", loginHint));
			}
		} else if ("card_primary_account_number".equals(format)) {
			// per PDF page 4
		} else {
			// RFC 9493 defines many formats, but let's at least log what we got
			log("login_hint format: " + format, args("login_hint", loginHint));
		}

		logSuccess("login_hint is a valid JSON object", args("login_hint", loginHint));
		return env;
	}

}
