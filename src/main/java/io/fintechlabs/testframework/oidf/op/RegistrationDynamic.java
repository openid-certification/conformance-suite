package io.fintechlabs.testframework.oidf.op;

import io.fintechlabs.testframework.condition.client.GetDynamicClientConfiguration;
import io.fintechlabs.testframework.sequence.client.DynamicallyRegisterClient;
import io.fintechlabs.testframework.sequence.client.LoadServerAndClientConfiguration;
import io.fintechlabs.testframework.testmodule.TestExecutionUnit;

/**
 * @author jricher
 *
 */

/*

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

*/

public class RegistrationDynamic extends OidcOpTestModule {

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.oidf.op.OidcOpTestModule#createConfigurationSequence()
	 */
	@Override
	protected TestExecutionUnit createConfigurationSequence() {
		return sequence(LoadServerAndClientConfiguration.class)
			.with("client_configuration",
				condition(GetDynamicClientConfiguration.class)
				);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.oidf.op.OidcOpTestModule#createStartSequence()
	 */
	@Override
	protected TestExecutionUnit createStartSequence() {
		return sequence(DynamicallyRegisterClient.class)

			;

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.oidf.op.OidcOpTestModule#createCallbackSequence()
	 */
	@Override
	protected TestExecutionUnit createCallbackSequence() {
		// TODO Auto-generated method stub
		return null;

	}
}
