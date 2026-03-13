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
import net.openid.conformance.sequence.AbstractConditionSequence;
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
	public ConditionSequence validateKeyAlgorithms() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(FAPIBrazilCheckKeyAlgInClientJWKs.class,
					ConditionResult.FAILURE, "BrazilOB-6.1");
			}
		};
	}

	@Override
	public ConditionSequence validateExpiresIn() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilValidateExpiresIn.class)
					.skipIfObjectMissing("expires_in")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirement("BrazilOB-5.2.2-12")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence validateIdTokenSigningAlg() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(FAPIBrazilValidateIdTokenSigningAlg.class,
					ConditionResult.FAILURE, "BrazilOB-6.1");
			}
		};
	}

	@Override
	public ConditionSequence setupResourceEndpointRequestBody(boolean brazilPayments) {
		if (!brazilPayments) {
			return null;
		}

		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// setup to call the payments initiation API, which requires a signed jwt request body
				call(sequenceOf(
					condition(CreateIdempotencyKey.class),
					condition(AddIdempotencyKeyHeader.class)));
				callAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetResourceMethodToPost.class);
				callAndStopOnFailure(CreatePaymentRequestEntityClaims.class);
				callAndStopOnFailure(AddEndToEndIdToPaymentRequestEntityClaims.class);

				// we reuse the request object conditions to add various jwt claims
				call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));

				// aud: the Resource Provider must validate if the value of the aud field matches the endpoint being triggered
				callAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");

				// iss: the receiver of the message shall validate if the value of the iss field matches the organisationId of the sender
				callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

				// jti: the value of the jti field shall be filled with the UUID defined by the institution according to RFC4122 version 4
				callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");

				// iat: the iat field shall be filled with the message generation time
				callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

				call(exec().unmapKey("request_object_claims"));

				callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
			}
		};
	}
}
