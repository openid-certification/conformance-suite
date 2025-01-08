package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddClientAssertionToPAREndpointParameters;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaimsWithIssAudience;
import net.openid.conformance.condition.client.SignClientAuthenticationAssertion;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateClientAuthenticationAssertionClaimsWithIssAudience.class, "PAR-2");
		callAndStopOnFailure(SignClientAuthenticationAssertion.class);
		callAndStopOnFailure(AddClientAssertionToPAREndpointParameters.class);
	}

}
