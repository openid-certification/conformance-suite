package net.openid.conformance.fapi1advancedfinal.dcr_no_authorization_flow;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilDCRHappyFlow;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "consents-bad-logged",
	displayName = "FAPI1-Advanced-Final: Brazil DCR happy flow without authentication flow",
	summary = "This test will try to use the recently created DCR to access either a Consents or, " +
		"if the server does not support Phase 2, a Payments Consent Call with a dummy, but well-formated payload to make sure " +
		"that the server will read the request but won’t be able to process it. \n" +
		"\u2022 Create a client by performing a DCR against the provided server - Expect Success \n" +
		"\u2022 Generate a token with the client_id created \n " +
		"\u2022 Use the token to call either the POST Consents or POST Payments Consents API, depending on the directory configuration provided \n" +
		"\u2022 Expect server to accept the message  but return a failure because with either 400 or 422 because of well formatted but invalid payload sent"
	,
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.consentUrl"
	}
)

public class DCRConsentsBadLoggedUser extends FAPI1AdvancedFinalBrazilDCRHappyFlow {

	protected ClientAuthType clientAuthType;

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		super.onPostAuthorizationFlowComplete();
	}

	@Override
	protected void configureClient() {
		clientAuthType = getVariant(ClientAuthType.class);
		super.configureClient();
	}

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

	@Override
	protected void callRegistrationEndpoint() {
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		eventLog.endBlock();

		eventLog.startBlock("Configuring dummy data");
		callAndStopOnFailure(AddDummyCPFToConfig.class);
		callAndStopOnFailure(AddDummyBusinessProductTypeToConfig.class);
		callAndStopOnFailure(AddDummyBrazilPaymentConsent.class);
		callAndStopOnFailure(ExtractResourceFromConfig.class);
		eventLog.endBlock();

		eventLog.startBlock("Checking consentURL");
		callAndStopOnFailure(EnsureConsentUrlIsNotNull.class);
		String consentUrl = env.getString("config", "resource.consentUrl");

		if(consentUrl.matches("^(https://)(.*?)(consents/v[0-9]/consents)")) {
			eventLog.startBlock("Calling Token Endpoint using Client Credentials");
			callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
			callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);

			call(callTokenEndpointShortVersion());
			eventLog.endBlock();

			eventLog.startBlock("Calling Consents API");
			call(consentsApiSequence());

		} else if(consentUrl.matches("^(https://)(.*?)(payments/v[0-9]/consents)")) {
			eventLog.startBlock("Calling Token Endpoint using Client Credentials");
			callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
			callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);

			call(callTokenEndpointShortVersion());
			eventLog.endBlock();
			eventLog.startBlock("Calling Payments Consents API");
			ConditionSequence paymentsConsentsSequence = new SignedPaymentConsentSequence()
				.insertAfter(AddFAPIAuthDateToResourceEndpointRequest.class, condition(FAPIBrazilCreatePaymentConsentRequest.class))
				.insertBefore(FAPIBrazilSignPaymentConsentRequest.class, condition(CopyClientJwksToClient.class))
				.skip(EnsureContentTypeApplicationJwt.class, "Not necessary since failure is expected")
				.replace(EnsureHttpStatusCodeIs201.class, condition(EnsureConsentResponseCodeWas422.class))
				.skip(ExtractSignedJwtFromResourceResponse.class, "Not necessary since failure is expected")
				.skip(FAPIBrazilValidateResourceResponseSigningAlg.class, "Not necessary since failure is expected")
				.skip(FAPIBrazilValidateResourceResponseTyp.class, "Not necessary since failure is expected")
				.skip(FAPIBrazilGetKeystoreJwksUri.class, "Not necessary since failure is expected")
				.skip(FetchServerKeys.class, "Not necessary since failure is expected")
				.skip(ValidateResourceResponseSignature.class, "Not necessary since failure is expected")
				.skip(ValidateResourceResponseJwtClaims.class, "Not necessary since failure is expected");

			call(paymentsConsentsSequence);


		}
		eventLog.endBlock();

	}

	private ConditionSequence consentsApiSequence(){
		return sequenceOf(
			condition(PrepareToPostConsentRequest.class),
			condition(AddConsentScope.class),
			condition(GetResourceEndpointConfiguration.class),
			condition(CreateEmptyResourceEndpointRequestHeaders.class),
			condition(AddFAPIAuthDateToResourceEndpointRequest.class),
			condition(FAPIBrazilCreateConsentRequest.class),
			condition(FAPIBrazilAddExpirationPlus30ToConsentRequest.class),
			condition(SetContentTypeApplicationJson.class),
			condition(CallConsentApiWithBearerToken.class).dontStopOnFailure().onFail(Condition.ConditionResult.INFO),
			condition(EnsureResponseCodeWas400.class).dontStopOnFailure().onFail(Condition.ConditionResult.FAILURE)
		);
	}

	private ConditionSequence callTokenEndpointShortVersion(){
		ConditionSequence sequence = sequenceOf(
			condition(AddClientIdToTokenEndpointRequest.class),
			condition(CreateClientAuthenticationAssertionClaims.class).dontStopOnFailure(),
			condition(SignClientAuthenticationAssertion.class).dontStopOnFailure(),
			condition(AddClientAssertionToTokenEndpointRequest.class).dontStopOnFailure(),
			condition(CallTokenEndpoint.class),
			condition(CheckIfTokenEndpointResponseError.class),
			condition(ExtractAccessTokenFromTokenResponse.class)
		);

		if (clientAuthType == ClientAuthType.MTLS) {
			sequence.skip(CreateClientAuthenticationAssertionClaims.class, "Not needed for MTLS")
				.skip(SignClientAuthenticationAssertion.class, "Not needed for MTLS")
				.skip(AddClientAssertionToTokenEndpointRequest.class, "Not needed for MTLS");
		}
		return sequence;
	}

	@Override
	protected void onPostAuthorizationFlowComplete(){
		// not needed as resource endpoint won't be called
	}
}
