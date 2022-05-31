package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.EnsureRequiredAuthorizationRequestParametersMatchRequestObject;

import java.util.Set;

public class EnsureRequiredBackchannelRequestParametersMatchRequestObject extends EnsureRequiredAuthorizationRequestParametersMatchRequestObject {

	@Override
	public Set<String> getParametersThatMustMatch() {
		return Set.of("client_id");
	}
}
