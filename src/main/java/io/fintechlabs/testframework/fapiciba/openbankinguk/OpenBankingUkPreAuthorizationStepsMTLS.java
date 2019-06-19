package io.fintechlabs.testframework.fapiciba.openbankinguk;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS;

public class OpenBankingUkPreAuthorizationStepsMTLS extends AbstractOpenBankingUkPreAuthorizationSteps {

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		// FIXME: can't call a condition sequence from a condition sequence
		// Copied from AddMTLSClientAuthenticationToTokenEndpointRequest:
		callAndContinueOnFailure(EnsureServerConfigurationSupportsMTLS.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}
}
