package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientIdToBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsMTLS;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddMTLSClientAuthenticationToBackchannelRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndContinueOnFailure(EnsureServerConfigurationSupportsMTLS.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		callAndStopOnFailure(AddClientIdToBackchannelAuthenticationEndpointRequest.class);

	}

}
