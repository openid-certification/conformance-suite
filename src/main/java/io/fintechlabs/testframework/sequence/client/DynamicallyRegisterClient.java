package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.CreateDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.SetDynamicRegistrationRequestGrantTypeToAuthorizationCode;
import io.fintechlabs.testframework.condition.client.UnregisterDynamicallyRegisteredClient;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

/**
 * @author jricher
 *
 */
public class DynamicallyRegisterClient extends AbstractConditionSequence {

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.sequence.ConditionSequence#evaluate()
	 */
	@Override
	public void evaluate() {
		// create basic dynamic registration request
		call(condition(CreateDynamicRegistrationRequest.class));

		runAccessory("registration_grant_type",
			condition(SetDynamicRegistrationRequestGrantTypeToAuthorizationCode.class));

		// Add in the redirect URIs needed for proper registration
		call(condition(AddRedirectUriToDynamicRegistrationRequest.class));

		call(condition(CallDynamicRegistrationEndpoint.class));

		runAccessory("validate_dynamic_registration");

		// IF management interface, delete the client to clean up
		runAccessory("unregister",
			condition(UnregisterDynamicallyRegisteredClient.class)
				.onSkip(ConditionResult.INFO)
				.skipIfStringsMissing("registration_client_uri", "registration_access_token"));
	}

}
