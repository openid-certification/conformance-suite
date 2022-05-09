package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CreateRefreshToken;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.consent.CreateNewConsentValidator;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.EnsureConsentResponseCodeWas201;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.EnsureRefreshTokenHasNotRotated;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.GenerateRefreshTokenRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "consents-api-delete-test",
	displayName = "Validate that clients cannot obtain one another's consents",
	summary = "Validates that clients cannot obtain one another's consents\n" +
		"\u2022 Creates a Consent with all of the existing permissions \n" +
		"\u2022 Calls the GET Consents with the Consent ID that has been created\n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Calls the Token endpoint using the 2nd client provided on the configuration file\n" +
		"\u2022 Calls the GET Consents with the first Consent ID created\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n" +
		"\u2022 Calls the DELETE Consents with the first Consent ID created, using the 2nd client\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n" +
		"\u2022 Calls the DELETE Consents with the first Consent ID created, using the 1st client\n" +
		"\u2022 Expects success on the Delete 20x\n" +
		"\u2022 Calls the GET Consents with the 1st Consent ID created\n" +
		"\u2022 Confirms that the Consent has been sent to a Rejected state",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"consent.productType"
	}
)
public class ConsentsApiDeleteTestModule extends AbstractFunctionalTestModule {

	protected ClientAuthType clientAuthType;

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddConsentScope.class);
		JsonObject client = env.getObject("client");
		//JsonElement scopeElement = client.get("scope");
		String scope = "openid consents accounts";
		client.addProperty("scope", scope);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		//callAndStopOnFailure(PrepareToPostConsentRequest.class);
		//callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class); TODO: probably add something here
		//TODO: set protected resourse to accounts
		clientAuthType = getVariant(ClientAuthType.class);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationConsentApi(addTokenEndpointClientAuthentication);
//			.replace(FAPIBrazilCreatePaymentConsentRequest.class, paymentConsentEditingSequence());

		return preauthSteps;
	}

	@Override
	protected void requestProtectedResource(){
		//callAndStopOnFailure(LoadOldAccessToken.class);
		eventLog.startBlock("Try calling protected resource after user authentication");
		callAndStopOnFailure(SaveProtectedResourceAccessToken.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
//		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
//		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		eventLog.startBlock("Deleting consent");
		callAndContinueOnFailure(LoadConsentsAccessToken.class);
		callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(SetResponseBodyOptional.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(ConsentWasRejectedOrDeleted.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(LoadProtectedResourceAccessToken.class);

		eventLog.startBlock("Try calling protected resource after consent is deleted");
		callAndStopOnFailure(CallProtectedResource.class);
		callAndContinueOnFailure(EnsureResponseCodeWas403or400.class, Condition.ConditionResult.FAILURE);

		eventLog.startBlock("Trying issuing a refresh token");
		ConditionSequence sequence = sequenceOf(
			condition(GenerateRefreshTokenRequest.class),
			condition(SetAccountScopeOnTokenEndpointRequest.class),
			condition(CreateClientAuthenticationAssertionClaims.class),
			condition(SignClientAuthenticationAssertion.class),
			condition(AddClientAssertionToTokenEndpointRequest.class),
			condition(CallTokenEndpoint.class)
			//condition(EnsureResponseCodeWas4xx.class).onFail(Condition.ConditionResult.FAILURE).dontStopOnFailure()
		);
		call(sequence);
	}

//	private ConditionSequence validateCreateConsentResponse(){
//		eventLog.startBlock("Validating create consent response");
//		callAndStopOnFailure(PrepareToPostConsentRequest.class);
//		callAndStopOnFailure(AddConsentScope.class);
//		callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
//		callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
//		callAndStopOnFailure(SetContentTypeApplicationJson.class);
//		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
//		callAndContinueOnFailure(CreateNewConsentValidator.class, Condition.ConditionResult.FAILURE);
//		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
//		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
//		callAndContinueOnFailure(CheckItemCountHasMin1.class);
//	}
//	@Override
//	protected void runTests() {
//
//		runInBlock("Validating create consent response", () -> {
//			callAndStopOnFailure(PrepareToPostConsentRequest.class);
//			callAndStopOnFailure(AddConsentScope.class);
//			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
//			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
//			callAndStopOnFailure(SetContentTypeApplicationJson.class);
//			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
//			callAndContinueOnFailure(CreateNewConsentValidator.class, Condition.ConditionResult.FAILURE);
//			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
//			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
//			callAndContinueOnFailure(CheckItemCountHasMin1.class);
//		});
//
//		runInBlock("Validating get consent response", () -> {
//			callAndStopOnFailure(ConsentIdExtractor.class);
//			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
//			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
//			callAndContinueOnFailure(ConsentDetailsIdentifiedByConsentIdValidator.class, Condition.ConditionResult.FAILURE);
//			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
//			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
//		});
//
////		runInBlock("Calling Token endpoint", () -> {
////			callAndStopOnFailure(CreateRefreshToken.class);
////			callAndStopOnFailure(CreateRefreshTokenRequest.class);
////			callAndStopOnFailure(CallTokenEndpoint.class);
////		});
//
////		runInBlock("Calling Token endpoint", () -> {
////			createAuthorizationCodeRequest();
////
////			// Store the original access token and ID token separately (see RefreshTokenRequestSteps)
////			env.mapKey("access_token", "first_access_token");
////			env.mapKey("id_token", "first_id_token");
////
////			callAndStopOnFailure(CallTokenEndpoint.class);
////			callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
////			callAndStopOnFailure(CheckForAccessTokenValue.class);
////			callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
////
////			// Set up the mappings for the refreshed access and ID tokens
////			env.mapKey("access_token", "second_access_token");
////			env.mapKey("id_token", "second_id_token");
////
////		});
////		runInBlock("Calling Token endpoint BEFORE DELETE", () -> {
////			ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication)
////				.replace(SetPaymentsScopeOnTokenEndpointRequest.class, condition(AddConsentScope.class));
//////				.replace(FAPIBrazilCreatePaymentConsentRequest.class, paymentConsentEditingSequence());
////			call(preauthSteps);
////		});
//
//		runInBlock("Deleting consent", () -> {
//			callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
//			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
//			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
//			callAndStopOnFailure(IgnoreResponseError.class);
//			callAndStopOnFailure(SetResponseBodyOptional.class);
//			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
//
//			callAndStopOnFailure(ConsentWasRejectedOrDeleted.class, Condition.ConditionResult.FAILURE);
//			callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.WARNING);
//		});
//
////		runInBlock("Calling Token endpoint", () -> {
////			callAndStopOnFailure(GetDynamicServerConfiguration.class);
////			callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);
////			callAndStopOnFailure(AddMTLSEndpointAliasesToEnvironment.class);
////			callAndStopOnFailure(GetStaticClientConfiguration.class);
////			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
////
////			callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
////			callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
////			call(sequence(clientAuthSequence));
////			callAndStopOnFailure(CallTokenEndpoint.class);
////			callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
////			callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
////		});
//	}
//
////	protected void createAuthorizationCodeRequest() {
////		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
////		if (addTokenEndpointClientAuthentication != null) {
////			call(sequence(addTokenEndpointClientAuthentication));
////		}
////	}
//
////	protected void requestAuthorizationCode() {
////		callAndStopOnFailure(CallTokenEndpoint.class);
////		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
////		callAndStopOnFailure(CheckForAccessTokenValue.class);
////		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
////
////		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, Condition.ConditionResult.INFO, "RFC6749-5.1"); // this is 'recommended' by the RFC, but we don't want to raise a warning on every test
////		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
////			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");
////
////		callAndContinueOnFailure(CheckForRefreshTokenValue.class);
////
////		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "OIDCC-3.1.3.3", "OIDCC-3.3.3.3");
////
////		// save the id_token returned from the token endpoint
////		env.putObject("token_endpoint_id_token", env.getObject("id_token"));
////
////		additionalTokenEndpointResponseValidation();
////
////		if (responseType.includesIdToken()) {
////			callAndContinueOnFailure(VerifyIdTokenSubConsistentHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
////		}
////	}
	@Override
	protected void validateResponse() {}
}
