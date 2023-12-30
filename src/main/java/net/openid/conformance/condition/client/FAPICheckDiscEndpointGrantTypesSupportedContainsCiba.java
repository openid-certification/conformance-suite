package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class FAPICheckDiscEndpointGrantTypesSupportedContainsCiba extends AbstractValidateJsonArray {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		String environmentVariable = "grant_types_supported";
		List<String> setValues = List.of("urn:openid:params:grant-type:ciba");
		int minimumMatchesRequired = 1;
		return validate(env, environmentVariable, setValues, minimumMatchesRequired,
			"The server does not support grant type urn:openid:params:grant-type:ciba");
	}

}
