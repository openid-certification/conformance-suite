package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.CheckCacheControlHeaderContainsNoStore;
import net.openid.conformance.vci10issuer.condition.VCIExtractDpopNonceFromNonceEndpointResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialNonceResponse;

public class ValidateVCINonceEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.WARNING, "OID4VCI-1FINAL-7.2");
		callAndContinueOnFailure(CheckCacheControlHeaderContainsNoStore.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndStopOnFailure(VCIValidateCredentialNonceResponse.class, "OID4VCI-1FINAL-7.2");
		// OID4VCI 7.2: the issuer MAY supply a DPoP nonce on the nonce response; if so, the wallet uses it in
		// the DPoP proof at the credential endpoint.
		callAndContinueOnFailure(VCIExtractDpopNonceFromNonceEndpointResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2", "DPOP-8.2");
	}
}
