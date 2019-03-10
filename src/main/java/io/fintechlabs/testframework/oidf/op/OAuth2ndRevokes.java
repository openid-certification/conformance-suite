package io.fintechlabs.testframework.oidf.op;

import io.fintechlabs.testframework.condition.client.CallProtectedResourceWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.ExtractUserInfoEndpointAsResource;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.TestExecutionUnit;

/**
 * @author jricher
 *
 */

/*

@PublishTestModule(testName = "OAuth2ndRevokes",
	displayName = "OAuth use access token to hit userinfo twice",
	configurationFields = {		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.client_secret"})

 */
public class OAuth2ndRevokes extends OAuth2nd {

	@Override
	protected ConditionSequence createConfigurationSequence() {
		return super.createConfigurationSequence()
			.then(condition(ExtractUserInfoEndpointAsResource.class));
	}

	@Override
	protected TestExecutionUnit createCallbackSequence() {

		return sequenceOf(
			processAuthorizationEndpointResponse(),

			createTokenEndpointResponseSequence(),

			exec().mapKey("resource", "userinfo_resource"),

			condition(CallProtectedResourceWithBearerToken.class),

			processAuthorizationEndpointResponse()
				.replace(CallTokenEndpoint.class, condition(CallTokenEndpointExpectingError.class)),

			condition(CallProtectedResourceWithBearerToken.class));
	}

}
