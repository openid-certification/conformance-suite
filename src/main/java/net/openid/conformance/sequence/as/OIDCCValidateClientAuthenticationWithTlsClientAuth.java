package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.EnsureMTLSRequestContainsValidClientId;
import net.openid.conformance.condition.as.ExtractClientCertificateFromTokenEndpointRequestHeaders;
import net.openid.conformance.condition.as.ExtractClientCredentialsFromFormPost;
import net.openid.conformance.condition.as.OIDCCExtractClientCertificateFromTokenEndpointRequestHeaders;
import net.openid.conformance.condition.as.ValidateClientCertificateForTlsClientAuth;
import net.openid.conformance.condition.as.ValidateClientIdAndSecret;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCValidateClientAuthenticationWithTlsClientAuth extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureMTLSRequestContainsValidClientId.class, Condition.ConditionResult.FAILURE, "RFC8705-2");
		callAndStopOnFailure(OIDCCExtractClientCertificateFromTokenEndpointRequestHeaders.class);
		callAndStopOnFailure(CheckForClientCertificate.class);
		callAndStopOnFailure(ValidateClientCertificateForTlsClientAuth.class, "RFC8705-2.1.2");
	}
}
