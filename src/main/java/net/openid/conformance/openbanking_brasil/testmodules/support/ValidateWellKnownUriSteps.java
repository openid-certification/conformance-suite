package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.client.CheckDiscEndpointDiscoveryUrl;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateWellKnownUriSteps extends AbstractConditionSequence {


	@Override
	public void evaluate() {
		call(exec().startBlock("Validating Well-Known URI"));
		callAndStopOnFailure(CheckDiscEndpointDiscoveryUrl.class);
		callAndStopOnFailure(CallDirectoryParticipantsEndpoint.class);
		call(exec().mapKey("resource_endpoint_response_full", "directory_participants_response_full"));
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		call(exec().unmapKey("resource_endpoint_response_full"));
		callAndStopOnFailure(EnsureWellKnownUriIsRegistered.class);
		call(exec().endBlock());

	}
}
