package io.fintechlabs.testframework.fapiciba.openbankinguk;

import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;

public class OpenBankingUkPreAuthorizationStepsPrivateKeyJwt extends AbstractOpenBankingUkPreAuthorizationSteps {

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		// FIXME: can't call a condition sequence from a condition sequence
		// Copied from AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest:
		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);
	}
}
