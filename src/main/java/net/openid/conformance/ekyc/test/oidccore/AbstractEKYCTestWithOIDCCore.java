package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.ekyc.condition.client.AddVerifiedClaimsToAuthorizationEndpointRequestUsingJsonNull;
import net.openid.conformance.ekyc.condition.client.AddUnverifiedClaimsToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromIdToken;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromUserinfoResponse;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInIdTokenAgainstOPMetadata;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInIdTokenAgainstRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoAgainstOPMetadata;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoResponseAgainstRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsRequestAgainstSchema;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsResponseAgainstSchema;
import net.openid.conformance.openid.AbstractOIDCCServerTest;
import net.openid.conformance.variant.AccessTokenSenderConstrainMethod;
import net.openid.conformance.variant.AuthRequestMethod;
import net.openid.conformance.variant.AuthRequestNonRepudiationMethod;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.SecurityProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;


@VariantParameters({
	SecurityProfile.class,
	AuthRequestMethod.class,
	AuthRequestNonRepudiationMethod.class,
	AccessTokenSenderConstrainMethod.class
})

@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none"
})

public abstract class AbstractEKYCTestWithOIDCCore extends AbstractOIDCCServerTest {

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
		processVerifiedClaimsInIdToken();
	}

	protected void processVerifiedClaimsInIdToken() {
		callAndStopOnFailure(ExtractVerifiedClaimsFromIdToken.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponseSchema();
		ensureReturnedVerifiedClaimsMatchOPMetadata(false);
		validateIdTokenVerifiedClaimsAgainstRequested();
	}

	protected void validateIdTokenVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(ValidateVerifiedClaimsInIdTokenAgainstRequest.class, Condition.ConditionResult.FAILURE, "IA-6");
	}

	protected void ensureReturnedVerifiedClaimsMatchOPMetadata(boolean isUserinfo) {
		if(isUserinfo){
			callAndContinueOnFailure(ValidateVerifiedClaimsInUserinfoAgainstOPMetadata.class, Condition.ConditionResult.FAILURE, "IA-9");
		} else {
			callAndContinueOnFailure(ValidateVerifiedClaimsInIdTokenAgainstOPMetadata.class, Condition.ConditionResult.FAILURE, "IA-9");
		}
	}

	protected void validateVerifiedClaimsResponseSchema() {
		callAndContinueOnFailure(ValidateVerifiedClaimsResponseAgainstSchema.class, Condition.ConditionResult.FAILURE, "IA-6");
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Userinfo endpoint tests");
		callAndStopOnFailure(CallProtectedResource.class);
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		processVerifiedClaimsInUserinfo();
		eventLog.endBlock();
	}

	protected void processVerifiedClaimsInUserinfo() {
		callAndContinueOnFailure(ExtractVerifiedClaimsFromUserinfoResponse.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponseSchema();
		ensureReturnedVerifiedClaimsMatchOPMetadata(true);
		validateUserinfoVerifiedClaimsAgainstRequested();
	}

	protected void validateUserinfoVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(ValidateVerifiedClaimsInUserinfoResponseAgainstRequest.class, Condition.ConditionResult.FAILURE, "IA-6");
	}

}
