package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.AddJARMResponseModeToServerConfiguration;
import net.openid.conformance.condition.as.AddResponseTypeCodeToServerConfiguration;
import net.openid.conformance.condition.as.par.AddPushedAuthorizationRequestEndpointToServerConfig;
import net.openid.conformance.condition.as.par.AddRequirePushedAuthorizationRequestsToServerConfig;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddJARMToServerConfiguration extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddResponseTypeCodeToServerConfiguration.class, "FAPI1-ADVANCED-5.2.2-2");
		callAndStopOnFailure(AddJARMResponseModeToServerConfiguration.class, "FAPI1-ADVANCED-5.2.2.2");
	}
}
