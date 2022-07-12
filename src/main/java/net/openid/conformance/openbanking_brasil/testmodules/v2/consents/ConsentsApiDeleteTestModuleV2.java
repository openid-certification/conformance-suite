package net.openid.conformance.openbanking_brasil.testmodules.v2.consents;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetCustomCustomerIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2.OpenBankingBrazilPreAuthorizationConsentApiV2;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.GenerateRefreshTokenRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "consents-api-delete-test-v2",
	displayName = "Makes sure that after consent has been deleted no more tokens can be issued with the related refresh token ",
	summary = "Makes sure that after consent has been deleted no more tokens can be issued with the related refresh token \n" +
		"\u2022 Creates a Consent with all of the existing permissions \n" +
		"\u2022 Redirects the user\n" +
		"\u2022 Calls the Token endpoint using the authorization code flow\n" +
		"\u2022 Makes sure a valid access token has been created\n" +
		"\u2022 Calls the Protected Resource endpoint to make sure a valid access token has been created\n" +
		"\u2022 Calls the DELETE Consents API\n" +
		"\u2022 Calls the Token endpoint to issue a new access token\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n" +
		"\u2022 Calls the Consents API with the initially authorized Consent \n" +
		"\u2022 Expects a 200 - Make sure Status is set to REJECTED. Make Sure RejectedBy is set to USER. Make sure " +
		"		Reason is set to \"CUSTOMER_MANUALLY_REVOKED”\n",
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
public class ConsentsApiDeleteTestModuleV2 extends AbstractFunctionalTestModule {

	protected ClientAuthType clientAuthType;

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildCustomCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void validateClientConfiguration() {
		super.validateClientConfiguration();
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(AddConsentScope.class);
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		clientAuthType = getVariant(ClientAuthType.class);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps = new OpenBankingBrazilPreAuthorizationConsentApiV2(addTokenEndpointClientAuthentication);

		return preauthSteps;
	}

	@Override
	protected void requestProtectedResource() {

		eventLog.startBlock("Try calling protected resource after user authentication");
		callAndStopOnFailure(PrepareToGetCustomCustomerIdentifications.class);
		callAndStopOnFailure(SaveProtectedResourceAccessToken.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
		eventLog.endBlock();

		eventLog.startBlock("Deleting consent");
		callAndContinueOnFailure(LoadConsentsAccessToken.class);
		callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(SetResponseBodyOptional.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(ConsentWasRejectedOrDeleted.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(LoadProtectedResourceAccessToken.class);

		eventLog.startBlock("Try calling protected resource after consent is deleted");
		callAndStopOnFailure(CallProtectedResource.class);
		callAndContinueOnFailure(EnsureResponseCodeWas403or400.class, Condition.ConditionResult.FAILURE);

		eventLog.startBlock("Trying issuing a refresh token");
		call(callTokenEndpointRefreshToken());

		eventLog.startBlock("Validating get consent response");
		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndStopOnFailure(TransformConsentRequestForProtectedResource.class);
		call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));
		preCallProtectedResource("Fetch consent");

		callAndStopOnFailure(EnsureConsentAspspRevoked.class);

	}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

	private ConditionSequence callTokenEndpointRefreshToken() {
		ConditionSequence sequence = sequenceOf(
			condition(GenerateRefreshTokenRequest.class),
			condition(CreateClientAuthenticationAssertionClaims.class),
			condition(SignClientAuthenticationAssertion.class),
			condition(AddClientAssertionToTokenEndpointRequest.class),
			condition(CallTokenEndpointAndReturnFullResponse.class).onFail(Condition.ConditionResult.WARNING).dontStopOnFailure(),
			condition(EnsureTokenResponseWasAFailure.class).onFail(Condition.ConditionResult.FAILURE).dontStopOnFailure()
		);

		if (clientAuthType == ClientAuthType.MTLS) {
			sequence.skip(CreateClientAuthenticationAssertionClaims.class, "Not needed for MTLS")
				.skip(SignClientAuthenticationAssertion.class, "Not needed for MTLS")
				.skip(AddClientAssertionToTokenEndpointRequest.class, "Not needed for MTLS");
		}
		return sequence;
	}

	@Override
	protected void validateResponse() {
	}
}
