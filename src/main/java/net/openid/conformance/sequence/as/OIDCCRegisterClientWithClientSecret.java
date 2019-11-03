package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.ExtractClientCredentialsFromBasicAuthorizationHeader;
import net.openid.conformance.condition.as.ValidateClientIdAndSecret;
import net.openid.conformance.condition.as.dynregistration.OIDCCRegisterClient;
import net.openid.conformance.condition.as.dynregistration.OIDCCSetDynamicClientPassword;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCRegisterClientWithClientSecret extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(OIDCCRegisterClient.class, "FIXME");
		callAndStopOnFailure(OIDCCSetDynamicClientPassword.class, "FIXME");
	}
}
