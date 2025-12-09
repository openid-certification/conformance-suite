package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractSetAuthzenApiEndpoint extends AbstractCondition {

	public Environment setAuthzenApiEndpoint(Environment env, String endpointName, String endpointConfigPath) {
		String endpointUrl = env.getString("pdp", endpointConfigPath);
		if(Strings.isNullOrEmpty(endpointUrl)) {
			throw error(endpointName + " URL is not set");
		}
		env.putString("authzen_api_endpoint", endpointUrl);
		logSuccess("Successfully set API endpoint", args(endpointName, endpointUrl));
		return env;
	}

}
