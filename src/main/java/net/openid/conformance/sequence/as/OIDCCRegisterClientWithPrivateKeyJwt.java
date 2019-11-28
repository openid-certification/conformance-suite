package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.dynregistration.OIDCCRegisterClient;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithPrivateKeyJwt extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(OIDCCRegisterClient.class);
		//TODO will add checks listed at https://gitlab.com/openid/conformance-suite/merge_requests/779#note_250447328 in another MR
	}
}
