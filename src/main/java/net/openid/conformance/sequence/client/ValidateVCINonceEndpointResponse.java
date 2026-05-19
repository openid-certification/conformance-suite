package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.CheckCacheControlHeaderContainsNoStore;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialNonceResponse;

public class ValidateVCINonceEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.WARNING, "OID4VCI-1FINAL-7.2");
		callAndContinueOnFailure(CheckCacheControlHeaderContainsNoStore.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndStopOnFailure(VCIValidateCredentialNonceResponse.class, "OID4VCI-1FINAL-7.2");
	}
}
