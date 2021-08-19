package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class OIDCCCheckDiscEndpointGrantTypesSupportedDynamic extends AbstractValidateJsonArray {

	private static final String environmentVariable = "grant_types_supported";

	private static final String[] SET_VALUES = { "authorization_code", "implicit" };
	private static final int minimumMatchesRequired = SET_VALUES.length;

	private static final String errorMessageNotEnough = "Servers certifying for the 'dynamic' certification profile are required to support the grant types 'authorization_code' and 'implicit'.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement grantTypesSupported = env.getElementFromObject("server", environmentVariable);

		if (grantTypesSupported == null) {
			logSuccess("server discovery document does not contain "+environmentVariable+", so by default authorization_code and implicit are supported");
			return env;
		}

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired, errorMessageNotEnough);

	}
}
