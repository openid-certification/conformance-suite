package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckIfBackchannelAuthenticationEndpointResponseError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!Strings.isNullOrEmpty(env.getString("backchannel_authentication_endpoint_response", "error"))) {
			throw error("Backchannel authentication endpoint error response", env.getObject("backchannel_authentication_endpoint_response"));
		} else {
			logSuccess("No error from Backchannel authentication endpoint");
			return env;
		}

	}

}
