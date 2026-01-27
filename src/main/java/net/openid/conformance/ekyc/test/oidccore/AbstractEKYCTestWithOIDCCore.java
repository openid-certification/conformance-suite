package net.openid.conformance.ekyc.test.oidccore;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToUserInfoEndpoint;
import net.openid.conformance.ekyc.condition.client.AddUnverifiedClaimsToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.AddVerifiedClaimsToAuthorizationEndpointRequestUsingJsonNull;
import net.openid.conformance.ekyc.condition.client.CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromIdToken;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromUserinfoResponse;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInIdTokenAgainstOPMetadata;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInIdTokenAgainstRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoAgainstOPMetadata;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoResponseAgainstRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsRequestAgainstSchema;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsResponseAgainstSchema;
import net.openid.conformance.openid.AbstractOIDCCServerSecurityProfileTest;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.EKYCVerifiedClaimsResponseSupport;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;


@VariantParameters({
	EKYCVerifiedClaimsResponseSupport.class
})

@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none"
})

public abstract class AbstractEKYCTestWithOIDCCore extends AbstractOIDCCServerSecurityProfileTest {

	private EKYCVerifiedClaimsResponseSupport eKYCVerifiedClaimsResponseSupport;

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		eKYCVerifiedClaimsResponseSupport = getVariant(EKYCVerifiedClaimsResponseSupport.class);
		env.putString("config", "ekyc.verified_claims_response_support", eKYCVerifiedClaimsResponseSupport.toString());
	}

	@Override
	protected void configureProtectedResourceUrl() {
		// Set Userinfo endpoint only if supported
		if(getVariant(EKYCVerifiedClaimsResponseSupport.class) != EKYCVerifiedClaimsResponseSupport.ID_TOKEN) {
			callAndContinueOnFailure(SetProtectedResourceUrlToUserInfoEndpoint.class, Condition.ConditionResult.WARNING);
		}
	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		// add claims
		// authorization_endpoint_request
		addUnverifiedClaimsToAuthorizationRequest();
		addVerifiedClaimsToAuthorizationRequest();
		validateVerifiedClaimsRequestSchema();
	}

	protected void addUnverifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest.class, Condition.ConditionResult.INFO);
		callAndContinueOnFailure(AddUnverifiedClaimsToAuthorizationEndpointRequest.class, Condition.ConditionResult.WARNING, "IA-6");
	}

	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddVerifiedClaimsToAuthorizationEndpointRequestUsingJsonNull.class, Condition.ConditionResult.WARNING, "IA-6");
	}

	protected void validateVerifiedClaimsRequestSchema() {
		callAndContinueOnFailure(ValidateVerifiedClaimsRequestAgainstSchema.class, Condition.ConditionResult.FAILURE, "IA-5.1");
	}

	@Override
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();
		if(eKYCVerifiedClaimsResponseSupport != EKYCVerifiedClaimsResponseSupport.USERINFO) {
			processVerifiedClaimsInIdToken();
		}
	}

	protected void processVerifiedClaimsInIdToken() {
		callAndStopOnFailure(ExtractVerifiedClaimsFromIdToken.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponseSchema();
		ensureReturnedVerifiedClaimsMatchOPMetadata(false);
		validateIdTokenVerifiedClaimsAgainstRequested();
	}

	protected void validateIdTokenVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(new ValidateVerifiedClaimsInIdTokenAgainstRequest(true), Condition.ConditionResult.FAILURE, "IA-6");
	}

	protected void ensureReturnedVerifiedClaimsMatchOPMetadata(boolean isUserinfo) {
		if(isUserinfo){
			callAndContinueOnFailure(ValidateVerifiedClaimsInUserinfoAgainstOPMetadata.class, Condition.ConditionResult.FAILURE, "IA-8");
		} else {
			callAndContinueOnFailure(ValidateVerifiedClaimsInIdTokenAgainstOPMetadata.class, Condition.ConditionResult.FAILURE, "IA-8");
		}
	}

	protected void validateVerifiedClaimsResponseSchema() {
		callAndContinueOnFailure(ValidateVerifiedClaimsResponseAgainstSchema.class, Condition.ConditionResult.FAILURE, "IA-6");
	}

	@Override
	protected void requestProtectedResource() {
		if(eKYCVerifiedClaimsResponseSupport != EKYCVerifiedClaimsResponseSupport.ID_TOKEN) {
			super.requestProtectedResource();
			processVerifiedClaimsInUserinfo();
		}
	}

	protected void processVerifiedClaimsInUserinfo() {
		callAndContinueOnFailure(ExtractVerifiedClaimsFromUserinfoResponse.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponseSchema();
		ensureReturnedVerifiedClaimsMatchOPMetadata(true);
		validateUserinfoVerifiedClaimsAgainstRequested();
	}

	protected void validateUserinfoVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(new ValidateVerifiedClaimsInUserinfoResponseAgainstRequest(true), Condition.ConditionResult.FAILURE, "IA-6");
	}

}
