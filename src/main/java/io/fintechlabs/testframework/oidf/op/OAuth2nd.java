package io.fintechlabs.testframework.oidf.op;

import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.sequence.client.AuthorizationEndpointRequestCodeIdToken;
import io.fintechlabs.testframework.sequence.client.CreateAuthorizationEndpointRequest;
import io.fintechlabs.testframework.sequence.client.LoadServerAndClientConfiguration;
import io.fintechlabs.testframework.sequence.client.ProcessAuthorizationEndpointResponse;
import io.fintechlabs.testframework.sequence.client.ProcessTokenEndpointResponse;
import io.fintechlabs.testframework.testmodule.Accessory;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestExecutionUnit;
import io.fintechlabs.testframework.testmodule.Variant;

/**
 * @author jricher
 *
 */
@PublishTestModule(testName = "OAuth2nd",
	displayName = "OAuth use Access Token twice",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.client_secret"},
	variants = {
		@Variant(name = "code_idtoken_private_key_jwks",
			accessories = {
			@Accessory(key = "response_type",
				sequences =
					AuthorizationEndpointRequestCodeIdToken.class
				)
			},
			configurationFields = {
				"server.discoveryUrl",
				"client.client_id",
				"client.scope",
				"client.jwks"
			}
		)
	}
)
public class OAuth2nd extends OidcOpTestModule {

	@Override
	protected ConditionSequence createConfigurationSequence() {
		return sequence(LoadServerAndClientConfiguration.class);
	}

	@Override
	protected ConditionSequence createStartSequence() {
		return sequence(CreateAuthorizationEndpointRequest.class);
	}

	@Override
	protected TestExecutionUnit createCallbackSequence() {

		return sequenceOf(
			processAuthorizationEndpointResponse(),
			createTokenEndpointResponseSequence(),
			processAuthorizationEndpointResponse()
				.replace(CallTokenEndpoint.class, condition(CallTokenEndpointExpectingError.class)));
	}

	protected ConditionSequence createTokenEndpointResponseSequence() {
		return sequence(ProcessTokenEndpointResponse.class);
	}

	protected ConditionSequence processAuthorizationEndpointResponse() {
		return sequence(ProcessAuthorizationEndpointResponse.class);
	}

}
