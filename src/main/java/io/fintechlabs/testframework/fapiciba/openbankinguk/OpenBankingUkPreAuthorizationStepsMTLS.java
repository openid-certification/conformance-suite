package io.fintechlabs.testframework.fapiciba.openbankinguk;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;

public class OpenBankingUkPreAuthorizationStepsMTLS extends AbstractOpenBankingUkPreAuthorizationSteps {

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		// FIXME: can't call a condition sequence from a condition sequence
		// Copied from AddMTLSClientAuthenticationToTokenEndpointRequest:
		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}
}
