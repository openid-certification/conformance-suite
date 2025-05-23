package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIVerifyIssuerStateInAuthorizationRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String expectedIssuerState = env.getString("vci","issuer_state");
		JsonObject paramsObject = env.getObject("par_endpoint_http_request_params");
		String actualIssuerState = OIDFJSON.getString(paramsObject.get("issuer_state"));

		if (!expectedIssuerState.equals(actualIssuerState)) {
			throw error("Issuer state mismatch", args("expected", expectedIssuerState, "actual", actualIssuerState));
		}
		logSuccess("Issuer state matches expected issuer state", args("expected", expectedIssuerState, "actual", actualIssuerState));

		return env;
	}
}
