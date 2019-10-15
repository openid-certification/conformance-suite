package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidateAuthenticationRequestId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject backchannelResponse = env.getObject("backchannel_authentication_endpoint_response");

		if (backchannelResponse == null || !backchannelResponse.isJsonObject()) {
			throw error("Backchannel Authentication Endpoint did not return a JSON object");
		}

		JsonElement authReqIdElement = backchannelResponse.get("auth_req_id");
		if (authReqIdElement == null) {
			throw error("auth_req_id in backchannel authentication endpoint can not be null.");
		}

		String authReqId = OIDFJSON.getString(authReqIdElement);
		if (Strings.isNullOrEmpty(authReqId)) {
			throw error("auth_req_id in backchannel authentication endpoint can not be empty.");
		}

		Matcher matcher = Pattern.compile("[A-Za-z0-9\\-_\\.]+").matcher(authReqId);
		if (!matcher.matches()) {
			throw error("auth_req_id contains characters other than A-Z, a-z, 0-9, '_', '-' and '.'.", args("auth_req_id", authReqId));
		}

		logSuccess("auth_req_id passed all validation checks");

		return env;
	}
}
