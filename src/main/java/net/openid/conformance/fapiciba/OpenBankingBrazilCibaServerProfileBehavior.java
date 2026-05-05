package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckCIBAModeIsPing;
import net.openid.conformance.condition.client.AddAudAsPaymentInitiationUriToRequestObject;
import net.openid.conformance.condition.client.AddEndToEndIdToPaymentRequestEntityClaims;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIdempotencyKeyHeader;
import net.openid.conformance.condition.client.AddIssAsCertificateOuToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.CreateIdempotencyKey;
import net.openid.conformance.condition.client.CreatePaymentRequestEntityClaims;
import net.openid.conformance.condition.client.EnsureAccessTokenValuesAreDifferent;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentInitiationRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateExpiresIn;
import net.openid.conformance.condition.client.SetApplicationJwtAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetHintTypeToLoginHint;
import net.openid.conformance.condition.client.SetResourceMethodToPost;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.sequence.client.RefreshTokenRequestSteps;
import net.openid.conformance.sequence.client.ValidateBrazilSignedResponse;

import java.util.function.Supplier;

public class OpenBankingBrazilCibaServerProfileBehavior extends FAPICIBAServerProfileBehavior {

	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return () -> {
			boolean isSecondClient = module.isSecondClient();
			boolean isDpop = false;
			boolean stopAfterConsentEndpoint = false;
			boolean payments = false;
			return new OpenBankingBrazilPreAuthorizationSteps(
				isSecondClient, isDpop, module.addTokenEndpointClientAuthentication, payments, false, stopAfterConsentEndpoint, false
			);
		};
	}

	@Override
	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return AbstractFAPICIBAID1.OpenBankingBrazilProfileAuthorizationEndpointSetupSteps.class;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return AbstractFAPICIBAID1.OpenBankingBrazilProfileIdTokenValidationSteps.class;
	}

	@Override
	public ConditionSequence onConfigure() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CheckCIBAModeIsPing.class, "BrazilCIBA-5.2.2");
				callAndStopOnFailure(SetHintTypeToLoginHint.class, "BrazilCIBA-5.2.2");
			}
		};
	}

	@Override
	public ConditionSequence validateExpiresIn() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilValidateExpiresIn.class)
					.skipIfObjectsMissing("expires_in")
					.onSkip(Condition.ConditionResult.INFO)
					.onFail(Condition.ConditionResult.FAILURE)
					.requirements("BrazilOB-5.2.2-13")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence setupResourceEndpointRequestBody() {
		boolean isPayments = false; // There's an option to add payments in a future iteration
		if (!isPayments) {
			return null;
		}

		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(sequenceOf(
					condition(CreateIdempotencyKey.class),
					condition(AddIdempotencyKeyHeader.class)));
				callAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetResourceMethodToPost.class);
				callAndStopOnFailure(CreatePaymentRequestEntityClaims.class);
				callAndStopOnFailure(AddEndToEndIdToPaymentRequestEntityClaims.class);

				call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));

				callAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");
				callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");
				callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
				callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

				call(exec().unmapKey("request_object_claims"));

				callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
			}
		};
	}

	@Override
	public ConditionSequence createUpdateResourceRequestSteps(boolean isSecondClient, Class<? extends ConditionSequence> addTokenEndpointClientAuthentication) {
		return new RefreshTokenRequestSteps(isSecondClient, addTokenEndpointClientAuthentication)
				.skip(EnsureAccessTokenValuesAreDifferent.class, "");
	}

	@Override
	public ConditionSequence validateResourceEndpointResponse() {
		if (module.scopeContains("payments")) {
			return ValidateBrazilSignedResponse.forResourceResponse();
		}
		return null;
	}
}
