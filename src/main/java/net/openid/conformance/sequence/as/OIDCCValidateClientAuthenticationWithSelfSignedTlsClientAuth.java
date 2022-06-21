package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.EnsureMTLSRequestContainsValidClientId;
import net.openid.conformance.condition.as.ExtractClientCertificateFromRequestHeaders;
import net.openid.conformance.condition.as.ValidateClientCertificateForSelfSignedTlsClientAuth;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCValidateClientAuthenticationWithSelfSignedTlsClientAuth extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureMTLSRequestContainsValidClientId.class, Condition.ConditionResult.FAILURE, "RFC8705-2");
		callAndStopOnFailure(ExtractClientCertificateFromRequestHeaders.class);
		callAndStopOnFailure(CheckForClientCertificate.class);
		callAndStopOnFailure(ValidateClientCertificateForSelfSignedTlsClientAuth.class, "RFC8705-2.2.2");
	}
}
