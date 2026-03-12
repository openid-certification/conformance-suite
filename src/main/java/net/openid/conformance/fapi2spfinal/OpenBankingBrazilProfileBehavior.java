package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddAudAsPaymentInitiationUriToRequestObject;
import net.openid.conformance.condition.client.AddEndToEndIdToPaymentRequestEntityClaims;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIdempotencyKeyHeader;
import net.openid.conformance.condition.client.AddIssAsCertificateOuToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.CreateIdempotencyKey;
import net.openid.conformance.condition.client.CreatePaymentRequestEntityClaims;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentInitiationRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateExpiresIn;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.SetApplicationJwtAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetResourceMethodToPost;
import net.openid.conformance.condition.common.FAPIBrazilCheckKeyAlgInClientJWKs;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * Profile behavior for OpenBanking Brazil.
 * Requires mTLS everywhere, Brazil-specific key algorithm validation, request object encryption,
 * Brazil-specific expires_in and id_token signing alg validation, and payments API support.
 */
public class OpenBankingBrazilProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return () -> module.createOBBPreauthSteps();
	}

	@Override
	public boolean shouldEncryptRequestObject(boolean isPar) {
		return !isPar;
	}

	@Override
	public void validateKeyAlgorithms() {
		module.doCallAndContinueOnFailure(FAPIBrazilCheckKeyAlgInClientJWKs.class,
			ConditionResult.FAILURE, "BrazilOB-6.1");
	}

	@Override
	public void validateExpiresIn() {
		module.doSkipIfMissing(new String[]{"expires_in"}, null, ConditionResult.INFO,
			FAPIBrazilValidateExpiresIn.class, ConditionResult.FAILURE,
			"BrazilOB-5.2.2-12");
	}

	@Override
	public void validateIdTokenSigningAlg() {
		module.doCallAndContinueOnFailure(FAPIBrazilValidateIdTokenSigningAlg.class,
			ConditionResult.FAILURE, "BrazilOB-6.1");
	}

	@Override
	public void setupResourceEndpointRequestBody(boolean brazilPayments) {
		if (!brazilPayments) {
			return;
		}

		// setup to call the payments initiation API, which requires a signed jwt request body
		module.doCall(module.doSequenceOf(
			module.doCondition(CreateIdempotencyKey.class),
			module.doCondition(AddIdempotencyKeyHeader.class)));
		module.doCallAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
		module.doCallAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
		module.doCallAndStopOnFailure(SetResourceMethodToPost.class);
		module.doCallAndStopOnFailure(CreatePaymentRequestEntityClaims.class);
		module.doCallAndStopOnFailure(AddEndToEndIdToPaymentRequestEntityClaims.class);

		// we reuse the request object conditions to add various jwt claims
		module.doCall(module.doExec().mapKey("request_object_claims", "resource_request_entity_claims"));

		// aud: the Resource Provider must validate if the value of the aud field matches the endpoint being triggered
		module.doCallAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");

		// iss: the receiver of the message shall validate if the value of the iss field matches the organisationId of the sender
		module.doCallAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

		// jti: the value of the jti field shall be filled with the UUID defined by the institution according to RFC4122 version 4
		module.doCallAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");

		// iat: the iat field shall be filled with the message generation time
		module.doCallAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

		module.doCall(module.doExec().unmapKey("request_object_claims"));

		module.doCallAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
	}
}
