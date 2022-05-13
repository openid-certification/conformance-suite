package net.openid.conformance.fapi1advancedfinal.dcr_no_authorization_flow;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToUserInfoClaims;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilDCRHappyFlow;
import net.openid.conformance.openbanking_brasil.consent.CreateNewConsentValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consents-bad-logged",
	displayName = "FAPI1-Advanced-Final: Brazil DCR happy flow without authentication flow",
	summary = "\u2022 Obtains a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration).\n" +
		"\u2022 Registers a new client on the target authorization server.",
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

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		super.onPostAuthorizationFlowComplete();
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

		eventLog.startBlock("Calling Token Endpoint using Client Credentials");
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
		callAndContinueOnFailure(CreateClientAuthenticationAssertionClaims.class);
		callAndContinueOnFailure(SignClientAuthenticationAssertion.class);
		callAndContinueOnFailure(AddClientAssertionToTokenEndpointRequest.class);
		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
		eventLog.endBlock();

		eventLog.startBlock("Configuring dummy data");
		callAndStopOnFailure(AddDummyCPFToConfig.class);
		callAndStopOnFailure(AddDummyBusinessProductTypeToConfig.class);
		eventLog.endBlock();

		eventLog.startBlock("Checking consentURL");
		callAndStopOnFailure(AddConsentUrlToConfig.class);
		String consentUrl = env.getString("config", "resource.consentUrl");

		if(consentUrl.matches("^(https://)(.*?)(consents/v[0-9]/consents)")) {
			eventLog.startBlock("Calling Consents API");
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(AddConsentScope.class);
			callAndStopOnFailure(GetResourceEndpointConfiguration.class);
			callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationPlus30ToConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			//callAndStopOnFailure(IgnoreResponseError.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.INFO);
			callAndStopOnFailure(EnsureConsentApiResponseCodeWas400.class, Condition.ConditionResult.FAILURE);
			//callAndStopOnFailure(EnsureResponseCodeWas403);
//		callAndContinueOnFailure(CreateNewConsentValidator.class, Condition.ConditionResult.FAILURE);
//		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
//		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(CheckItemCountHasMin1.class);
		} else if(consentUrl.matches("^(https://)(.*?)(payments/v[0-9]/consents)")) {
			eventLog.startBlock("Calling Payments Consents API");
		}
		eventLog.endBlock();

	}

	@Override
	protected void onPostAuthorizationFlowComplete(){
		// not needed as resource endpoint won't be called
	}
}
