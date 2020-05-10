package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.StringJoiner;

public class OIDCCAddAcrValuesToAuthorizationEndpointRequest extends AbstractCondition {

	public static final String ACR_VALUES_SUPPORTED = "acr_values_supported";

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "server" } )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		var acrValuesSupported = env.getElementFromObject("server", ACR_VALUES_SUPPORTED);
		var acrValues = env.getString("server", "acr_values");
		String acrValuesRequest;
		String msg;
		if (acrValuesSupported != null) {
			// include all values server supports
			StringJoiner sj = new StringJoiner(" ");

			for (JsonElement jsonElementAcrValue : acrValuesSupported.getAsJsonArray()) {
				sj.add(OIDFJSON.getString(jsonElementAcrValue));
			}
			acrValuesRequest = sj.toString();

			msg = "Added all acr values from server discovery document's "+ACR_VALUES_SUPPORTED+"to acr_values in authorization endpoint request.";
		} else if (!Strings.isNullOrEmpty(acrValues)){
			// server doesn't support discovery; use user supplied value from test config
			acrValuesRequest = acrValues;
			msg = "Added acr_values from test configuration";
		} else {
			// include just '1' and '2' as per https://github.com/rohe/oidctest/blob/a306ff8ccd02da456192b595cf48ab5dcfd3d15a/src/oidctest/op/func.py#L365
			acrValuesRequest = "1 2";
			msg = "server discovery document does not contain "+ACR_VALUES_SUPPORTED+" (or, for static server config, test configuration does not contain acr_values) so setting acr_values in authorization endpoint request to '1 2'.";
		}

		authorizationEndpointRequest.addProperty("acr_values", acrValuesRequest);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess(msg, args("request", authorizationEndpointRequest, "acr_values", acrValuesRequest));

		return env;
	}
}
