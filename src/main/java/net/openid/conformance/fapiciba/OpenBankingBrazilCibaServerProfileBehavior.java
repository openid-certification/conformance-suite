package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckCIBAModeIsPing;
import net.openid.conformance.condition.client.AddAudAsPaymentInitiationUriToRequestObject;
import net.openid.conformance.condition.client.AddEndToEndIdToPaymentRequestEntityClaims;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIdempotencyKeyHeader;
import net.openid.conformance.condition.client.AddIssAsCertificateOuToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.CallProtectedResource;
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

import java.util.function.Supplier;

public class OpenBankingBrazilCibaServerProfileBehavior extends FAPICIBAServerProfileBehavior {

	@Override
	public Class<? extends ConditionSequence> getResourceConfiguration() {
		return AbstractFAPICIBAID1.FAPIResourceConfiguration.class;
	}

	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return () -> {
			boolean isSecondClient = module.isSecondClient();
			boolean isDpop = false;
			boolean isBrazilOpenInsurance = false;
			boolean stopAfterConsentEndpoint = false;
			boolean payments = false;
			return new OpenBankingBrazilPreAuthorizationSteps(
				isSecondClient, isDpop, module.addTokenEndpointClientAuthentication, payments, isBrazilOpenInsurance, stopAfterConsentEndpoint, false
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
	public void applyProfileSpecificServerConfigChecks() {
		module.callCondition(CheckCIBAModeIsPing.class, "BrazilCIBA-5.2.2");
		module.callCondition(SetHintTypeToLoginHint.class, "BrazilCIBA-5.2.2");
	}

	@Override
	public void validateProfileSpecificTokenEndpointExpiresIn() {
		module.callConditionSkipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			FAPIBrazilValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-13");
	}

	@Override
	public void applyProfileSpecificResourceEndpointSetup() {
		boolean isPayments = false; // There's an option to add payments in a future iteration
		if (isPayments) {
			module.callSequence(new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					call(condition(CreateIdempotencyKey.class));
					call(condition(AddIdempotencyKeyHeader.class));
				}
			});
			module.callCondition(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
			module.callCondition(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
			module.callCondition(SetResourceMethodToPost.class);
			module.callCondition(CreatePaymentRequestEntityClaims.class);
			module.callCondition(AddEndToEndIdToPaymentRequestEntityClaims.class);

			getEnv().mapKey("request_object_claims", "resource_request_entity_claims");

			module.callCondition(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");
			module.callCondition(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");
			module.callCondition(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
			module.callCondition(AddIatToRequestObject.class, "BrazilOB-6.1");

			getEnv().unmapKey("request_object_claims");

			module.callCondition(FAPIBrazilSignPaymentInitiationRequest.class);
		}
	}

	@Override
	public void applyProfileSpecificResourceEndpointRetry(boolean isSecondClient, Class<? extends ConditionSequence> addTokenEndpointClientAuthentication) {
		ConditionSequence sequence =
			new RefreshTokenRequestSteps(isSecondClient, addTokenEndpointClientAuthentication)
				.skip(EnsureAccessTokenValuesAreDifferent.class, "");

		int httpStatus = getEnv().getInteger("endpoint_response", "status");
		for(int i = 0; i < 3 && httpStatus == 401; i++) {
			module.callSequence(sequence);
			module.callCondition(CallProtectedResource.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");
			httpStatus = getEnv().getInteger("endpoint_response", "status");
		}
	}
}
