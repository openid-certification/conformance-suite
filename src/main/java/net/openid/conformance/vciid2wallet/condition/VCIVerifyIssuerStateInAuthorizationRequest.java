package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIVerifyIssuerStateInAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment
	public Environment evaluate(Environment env) {

		String expectedIssuerState = env.getString("vci","issuer_state");
		if (expectedIssuerState == null) {
			throw error("Missing expected issuer in env");
		}

		JsonObject paramsObject = env.getObject("par_endpoint_http_request_params");
		JsonElement actualIssuerStateEl = paramsObject.get("issuer_state");
		if (actualIssuerStateEl == null) {
			throw error("Missing issuer_state in http_request_params", args("par_endpoint_http_request_params", paramsObject));
		}

		String actualIssuerState = OIDFJSON.getString(actualIssuerStateEl);

		if (!expectedIssuerState.equals(actualIssuerState)) {
			throw error("Issuer state mismatch", args("expected", expectedIssuerState, "actual", actualIssuerState));
		}
		logSuccess("Issuer state matches expected issuer state", args("expected", expectedIssuerState, "actual", actualIssuerState));

		return env;
	}
}
