package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.Environment;

/**
 * This class constructs the form encoded Request Object for a PAR request.
 */
public class BuildUnsignedRequestToDirectPostEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = "direct_post_request_form_parameters")
	public Environment evaluate(Environment env) {


		var o = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		env.putObject("direct_post_request_form_parameters", o);

		logSuccess("Created direct post response", o);

		return env;
	}

}
