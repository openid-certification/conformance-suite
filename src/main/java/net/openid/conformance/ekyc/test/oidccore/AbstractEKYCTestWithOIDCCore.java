package net.openid.conformance.ekyc.test.oidccore;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CreateTransactionId;
import net.openid.conformance.condition.client.AddEssentialAcrClaimWithNoValuesToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddTransactionIdToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToUserInfoEndpoint;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdTokenFromTokenResponseEncryption;
import net.openid.conformance.condition.client.VerifyIdTokenSubConsistentHybridFlow;
import net.openid.conformance.ekyc.condition.client.AddUnverifiedClaimsToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.AddVerifiedClaimsToAuthorizationEndpointRequestUsingJsonNull;
import net.openid.conformance.ekyc.condition.client.CheckForUnexpectedPropertiesInVerifiedClaimsRequest;
import net.openid.conformance.ekyc.condition.client.CheckForUnexpectedPropertiesInVerifiedClaimsResponse;
import net.openid.conformance.ekyc.condition.client.CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromIdToken;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromUserinfoResponse;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInIdTokenAgainstOPMetadata;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInIdTokenAgainstRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoAgainstOPMetadata;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoResponseAgainstRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsRequestAgainstCustomSchemas;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsRequestAgainstSchema;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsResponseAgainstCustomSchemas;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsResponseAgainstSchema;
import net.openid.conformance.openid.AbstractOIDCCServerSecurityProfileTest;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.EKYCProfile;
import net.openid.conformance.variant.EKYCVerifiedClaimsResponseSupport;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;


@ConfigurationFields({
	"ekyc.unverified_claims_names",
	"ekyc.verified_claims_names",
	"ekyc.request_schemas",
	"ekyc.response_schemas",
})
@VariantParameters({
	EKYCVerifiedClaimsResponseSupport.class,
	EKYCProfile.class,
})

@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none"
})

public abstract class AbstractEKYCTestWithOIDCCore extends AbstractOIDCCServerSecurityProfileTest {

	private EKYCVerifiedClaimsResponseSupport eKYCVerifiedClaimsResponseSupport;
	protected Class <? extends ConditionSequence> profileAddUnverifiedClaimsSteps = null;
	protected Class <? extends ConditionSequence> profileAddVerifiedClaimsSteps = null;

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

	@VariantSetup(parameter = EKYCProfile.class, value = "select_id")
	public void setupProfileAuthorizationEndpointSetupSteps() {
		profileAuthorizationEndpointSetupSteps = SelectIdAuthorizationEndpointSetupSteps.class;
		profileAddUnverifiedClaimsSteps = SelectIdAddUnverifiedClaimsSteps.class;
	}

	protected void addUnverifiedClaimsToAuthorizationRequest() {
		callAndStopOnFailure(CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(AddUnverifiedClaimsToAuthorizationEndpointRequest.class, Condition.ConditionResult.WARNING, "IA-5.3", "IA-7");
		if(profileAddUnverifiedClaimsSteps!=null) {
			call(sequence(profileAddUnverifiedClaimsSteps));
		}
	}

	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddVerifiedClaimsToAuthorizationEndpointRequestUsingJsonNull.class, Condition.ConditionResult.WARNING, "IA-5.3", "IA-7");
		if(profileAddVerifiedClaimsSteps!=null) {
			call(sequence(profileAddVerifiedClaimsSteps));
		}
	}

	protected void validateVerifiedClaimsRequestSchema() {
		callAndContinueOnFailure(ValidateVerifiedClaimsRequestAgainstSchema.class, Condition.ConditionResult.FAILURE, "IA-5.1");
		callAndContinueOnFailure(CheckForUnexpectedPropertiesInVerifiedClaimsRequest.class, Condition.ConditionResult.WARNING, "IA-5.1");
		callAndContinueOnFailure(ValidateVerifiedClaimsRequestAgainstCustomSchemas.class, Condition.ConditionResult.FAILURE);
	}

	@Override
	protected void requestAuthorizationCode() {
		callTokenEndpoint();
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		if(!getVariant(EKYCProfile.class).equals(EKYCProfile.SELECT_ID) || (eKYCVerifiedClaimsResponseSupport != EKYCVerifiedClaimsResponseSupport.ID_TOKEN)) {
			callAndStopOnFailure(CheckForAccessTokenValue.class);
			callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

			callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, Condition.ConditionResult.INFO, "RFC6749-5.1"); // this is 'recommended' by the RFC, but we don't want to raise a warning on every test
			skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
				ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

			callAndContinueOnFailure(CheckForRefreshTokenValue.class, Condition.ConditionResult.INFO);
		} else {
			// SelectID or ID Token only support for Verified Claims skips Access Token checks
			// NOT Standard OAuth/OIDC
			eventLog.log(getName(), "Skipping Access Token Checks due to ID Token only support");
		}

		skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO,
			ValidateIdTokenFromTokenResponseEncryption.class, Condition.ConditionResult.WARNING, "OIDCC-10.2");
		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "OIDCC-3.1.3.3", "OIDCC-3.3.3.3");

		// save the id_token returned from the token endpoint
		env.putObject("token_endpoint_id_token", env.getObject("id_token"));

		additionalTokenEndpointResponseValidation();

		if (responseType.includesIdToken()) {
			callAndContinueOnFailure(VerifyIdTokenSubConsistentHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
		}
	}

	@Override
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();
		if(eKYCVerifiedClaimsResponseSupport != EKYCVerifiedClaimsResponseSupport.USERINFO) {
			processVerifiedClaimsInIdToken();
		}
	}

	// id_token is processed before userinfo; validateVerifiedClaimsResponseSchema validates
	// whichever location is present (preferring userinfo). This ordering ensures id_token
	// is validated here before userinfo is extracted, then userinfo is validated separately.
	protected void processVerifiedClaimsInIdToken() {
		callAndStopOnFailure(ExtractVerifiedClaimsFromIdToken.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponseSchema();
		ensureReturnedVerifiedClaimsMatchOPMetadata(false);
		validateIdTokenVerifiedClaimsAgainstRequested();
	}

	protected void validateIdTokenVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(new ValidateVerifiedClaimsInIdTokenAgainstRequest(true), Condition.ConditionResult.FAILURE, "IA-5.7", "IA-7");
	}

	protected void ensureReturnedVerifiedClaimsMatchOPMetadata(boolean isUserinfo) {
		if(isUserinfo){
			callAndContinueOnFailure(ValidateVerifiedClaimsInUserinfoAgainstOPMetadata.class, Condition.ConditionResult.FAILURE, "IA-8");
		} else {
			callAndContinueOnFailure(ValidateVerifiedClaimsInIdTokenAgainstOPMetadata.class, Condition.ConditionResult.FAILURE, "IA-8");
		}
	}

	protected void validateVerifiedClaimsResponseSchema() {
		callAndContinueOnFailure(ValidateVerifiedClaimsResponseAgainstSchema.class, Condition.ConditionResult.FAILURE, "IAVC-5");
		callAndContinueOnFailure(CheckForUnexpectedPropertiesInVerifiedClaimsResponse.class, Condition.ConditionResult.WARNING, "IAVC-5");
		callAndContinueOnFailure(ValidateVerifiedClaimsResponseAgainstCustomSchemas.class, Condition.ConditionResult.FAILURE);
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
		callAndContinueOnFailure(new ValidateVerifiedClaimsInUserinfoResponseAgainstRequest(true), Condition.ConditionResult.FAILURE, "IA-5.7", "IA-7");
	}

	public static class SelectIdAuthorizationEndpointSetupSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateTransactionId.class);
			callAndStopOnFailure(AddTransactionIdToAuthorizationEndpointRequest.class);
			call(exec().exposeEnvironmentString("transaction_id"));
		}
	}

	public static class SelectIdAddUnverifiedClaimsSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddEssentialAcrClaimWithNoValuesToAuthorizationEndpointRequest.class);
		}
	}

}
