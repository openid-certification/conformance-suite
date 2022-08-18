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
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "consents-api-delete-test-v2",
	displayName = "Makes sure that after consent has been deleted no more tokens can be issued with the related refresh token ",
	summary = "Make sure that after consent has been deleted, no more tokens can be used or issued using a refresh token \n" +
		"\u2022 Creates a Consent with all of the existing permissions \n" +
		"\u2022 Redirects the user\n" +
		"\u2022 Calls the Token endpoint using the authorization code flow\n" +
		"\u2022 Makes sure a valid access token has been created\n" +
		"\u2022 Calls the Protected Resource endpoint to make sure a valid access token has been created\n" +
		"\u2022 Calls the DELETE Consents API Calls the Consents API with the initially authorized Consent - Expects a 204\n" +
		"\u2022 Calls the Consents API - Make sure Status is set to REJECTED. Make Sure RejectedBy is set to USER. Make sure Reason is set to \"CUSTOMER_MANUALLY_REVOKED\"\n"+
		"\u2022 Call the protected resource with the access token previously issued - expects a 401\n" +
		"\u2022 Call the token endpoint to issue a new access token using the refresh token - expects a 401\n",
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
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class ConsentsApiDeleteTestModuleV2 extends AbstractFunctionalTestModule {

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
		ConditionSequence preauthSteps = new OpenBankingBrazilPreAuthorizationConsentApiV2(addTokenEndpointClientAuthentication, false);

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
		callAndStopOnFailure(EnsureConsentResponseWas204.class);

		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(SetResponseBodyOptional.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(EnsureConsentResponseWas200.class);
		callAndContinueOnFailure(EnsureConsentAspspRevoked.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(LoadProtectedResourceAccessToken.class);

		eventLog.startBlock("Try calling protected resource after consent is deleted");
		callAndStopOnFailure(CallProtectedResource.class);
		callAndContinueOnFailure(EnsureResponseCodeWas401.class, Condition.ConditionResult.FAILURE);

		eventLog.startBlock("Trying issuing a refresh token");
		call(callTokenEndpointRefreshToken());
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
			condition(EnsureTokenResponseWas400.class).onFail(Condition.ConditionResult.FAILURE).dontStopOnFailure(),
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
