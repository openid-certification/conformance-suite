package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.GenerateRefreshTokenRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "consents-api-delete-test",
	displayName = "Makes sure that after consent has been deleted no more tokens can be issued with the related refresh token ",
	summary = "Makes sure that after consent has been deleted no more tokens can be issued with the related refresh token \n" +
		"\u2022 Creates a Consent with all of the existing permissions \n" +
		"\u2022 Redirects the user\n" +
		"\u2022 Calls the Token endpoint using the authorization code flow\n" +
		"\u2022 Makes sure a valid access token has been created\n" +
		"\u2022 Calls the Protected Resource endpoint to make sure a valid access token has been created\n" +
		"\u2022 Call the DELETE Consents API\n" +
		"\u2022 Calls the Token endpoint to issue a new access token\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n",
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
		super.validateClientConfiguration();
		callAndStopOnFailure(AddConsentScope.class);
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		clientAuthType = getVariant(ClientAuthType.class);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationConsentApi(addTokenEndpointClientAuthentication);

		return preauthSteps;
	}

	@Override
	protected void requestProtectedResource(){

		eventLog.startBlock("Try calling protected resource after user authentication");
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
			condition(CallTokenEndpoint.class),
			condition(EnsureTokenResponseCodeWas403or400.class).onFail(Condition.ConditionResult.FAILURE).dontStopOnFailure()
		);
		call(sequence);
	}

	@Override
	protected void validateResponse() {}
}
