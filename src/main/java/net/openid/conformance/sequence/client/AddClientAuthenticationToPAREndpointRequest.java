package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddClientIdToPAREndpointRequest;
import net.openid.conformance.condition.client.BuildMTLSRequestObjectPostToPAREndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectPostToPAREndpoint;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.SignClientAuthenticationAssertion;
import net.openid.conformance.condition.client.UpdateClientAuthenticationAssertionClaimsWithISSAud;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddClientAuthenticationToPAREndpointRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);
		callAndStopOnFailure(UpdateClientAuthenticationAssertionClaimsWithISSAud.class, "PAR-2");
		callAndStopOnFailure(SignClientAuthenticationAssertion.class);
		callAndStopOnFailure(BuildRequestObjectPostToPAREndpoint.class);
	}

}
