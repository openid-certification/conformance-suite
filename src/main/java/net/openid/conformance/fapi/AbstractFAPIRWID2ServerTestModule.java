package net.openid.conformance.fapi;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.EnsureMinimumKeyLength;
import net.openid.conformance.condition.as.ValidateClientSigningKeySize;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddFAPIFinancialIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddRedirectUriQuerySuffix;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenExpectingError;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForSubjectInIdToken;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckMatchingStateParameter;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenLength;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeLength;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenLength;
import net.openid.conformance.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAtHash;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponseFromJARMResponse;
import net.openid.conformance.condition.client.ExtractCHash;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractJARMFromURLQuery;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ExtractSHash;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromOBResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import net.openid.conformance.condition.client.FAPIGenerateResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.FAPIValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.RedirectQueryTestDisabled;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.RejectNonJarmResponsesInUrlQuery;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToJWT;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToAccountsEndpoint;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.ValidateAtHash;
import net.openid.conformance.condition.client.ValidateCHash;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdToken;
import net.openid.conformance.condition.client.ValidateIdTokenACRClaimAgainstRequest;
import net.openid.conformance.condition.client.ValidateIdTokenNonce;
import net.openid.conformance.condition.client.ValidateIdTokenSignature;
import net.openid.conformance.condition.client.ValidateIdTokenSignatureUsingKid;
import net.openid.conformance.condition.client.ValidateJARMExpRecommendations;
import net.openid.conformance.condition.client.ValidateJARMResponse;
import net.openid.conformance.condition.client.ValidateJARMSignatureUsingKid;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.client.ValidateSHash;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.client.ValidateSuccessfulHybridResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.ValidateSuccessfulJARMResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.common.CheckForKeyIdInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12;
import net.openid.conformance.condition.common.FAPICheckKeyAlgInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.sequence.client.AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.sequence.client.FAPIAuthorizationEndpointSetup;
import net.openid.conformance.sequence.client.OpenBankingUkAuthorizationEndpointSetup;
import net.openid.conformance.sequence.client.OpenBankingUkPreAuthorizationSteps;
import net.openid.conformance.sequence.client.ValidateOpenBankingUkIdToken;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPIProfile;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

import java.util.function.Supplier;

@VariantParameters({
	ClientAuthType.class,
	FAPIProfile.class,
	FAPIResponseMode.class
})
@VariantConfigurationFields(parameter = FAPIProfile.class, value = "openbanking_uk", configurationFields = {
	"resource.resourceUrlAccountRequests",
	"resource.resourceUrlAccountsResource"
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
public abstract class AbstractFAPIRWID2ServerTestModule extends AbstractRedirectServerTestModule {

	private int whichClient;
	protected boolean jarm = false;

	// for variants to fill in by calling the setup... family of methods
	private Class<? extends ConditionSequence> resourceConfiguration;
	protected Class<? extends ConditionSequence> addTokenEndpointClientAuthentication;
	private Supplier<? extends ConditionSequence> preAuthorizationSteps;
	protected Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;
	private Class<? extends ConditionSequence> profileIdTokenValidationSteps;

	public static class FAPIResourceConfiguration extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		}
	}

	public static class OpenBankingUkResourceConfiguration extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetProtectedResourceUrlToAccountsEndpoint.class);
		}
	}

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return;
		}

		jarm = getVariant(FAPIResponseMode.class) == FAPIResponseMode.JARM;

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
		callAndContinueOnFailure(EnsureMinimumKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		whichClient = 1;

		// Set up the client configuration
		configureClient();

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		call(sequence(resourceConfiguration));

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
		callAndContinueOnFailure(ExtractTLSTestValuesFromOBResourceConfiguration.class, Condition.ConditionResult.INFO);

		callAndStopOnFailure(FAPIGenerateResourceEndpointRequestHeaders.class);
		// This header is no longer mentioned in the FAPI standard as of ID2, however the UK OB spec most banks are
		// using (v3.1.1) erroneously requires that this header is sent in all cases, so for now we send it in all cases
		// (even pure FAPI-RW, as it's hard to arrange otherwise).
		callAndStopOnFailure(AddFAPIFinancialIdToResourceEndpointRequest.class);

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void onConfigure(JsonObject config, String baseUrl) {

		// No custom configuration
	}

	protected void configureClient() {
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		// Test won't pass without MATLS, but we'll try anyway (for now)
		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);

		validateClientConfiguration();

		eventLog.startBlock("Verify configuration of second client");

		// extract second client
		switchToSecondClient();
		callAndStopOnFailure(GetStaticClient2Configuration.class);
		callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);

		validateClientConfiguration();

		unmapClient();

		eventLog.endBlock();
	}

	protected void validateClientConfiguration() {
		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");
		callAndContinueOnFailure(ValidateClientSigningKeySize.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);
	}

	/* (non-Javadoc)
	 * @see TestModule#start()
	 */
	@Override
	public void start() {

		setStatus(Status.RUNNING);

		performAuthorizationFlow();
	}

	protected void performPreAuthorizationSteps() {
		if (preAuthorizationSteps != null) {
			call(sequence(preAuthorizationSteps));
		}
	}

	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");

		createAuthorizationRequest();

		createAuthorizationRedirect();

		performRedirect();
	}

	protected void createAuthorizationRequest() {

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		performProfileAuthorizationEndpointSetup();

		if (isSecondClient()) {
			env.putInteger("requested_state_length", 128);
		} else {
			env.putInteger("requested_state_length", null);
		}

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		if (jarm) {
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToJWT.class);
		} else {
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
		}
	}

	protected void performProfileAuthorizationEndpointSetup() {
		call(sequence(profileAuthorizationEndpointSetupSteps));
	}

	protected void createAuthorizationRedirect() {

		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		if (isSecondClient()) {
			callAndStopOnFailure(AddIatToRequestObject.class);
		}
		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(AddAudToRequestObject.class);

		callAndStopOnFailure(AddIssToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	protected void onAuthorizationCallbackResponse() {

		callAndStopOnFailure(CheckMatchingCallbackParameters.class);

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		if (jarm) {
			callAndContinueOnFailure(ValidateSuccessfulJARMResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING);
		} else {
			callAndContinueOnFailure(ValidateSuccessfulHybridResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING);
		}

		callAndContinueOnFailure(CheckMatchingStateParameter.class, ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		handleSuccessfulAuthorizationEndpointResponse();
	}

	// This is only used for the id token from the authorization endpoint, the token endpoint one is verified
	// separately (I'm not sure why)
	protected void performIdTokenValidation() {

		callAndContinueOnFailure(ValidateIdToken.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, ConditionResult.FAILURE,"OIDCC-2");

		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");

		performProfileIdTokenValidation();

		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		// This condition is a warning because we're not yet 100% sure of the code
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, ConditionResult.WARNING, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "FAPI-RW-8.6");
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {

		if (!jarm) {
			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-RW-5.2.2-3");

			// save the id_token returned from the authorisation endpoint
			env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));
			performIdTokenValidation();

			callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

			skipIfMissing(new String[]{"s_hash"}, null, Condition.ConditionResult.INFO,
				ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

			callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			skipIfMissing(new String[]{"c_hash"}, null, Condition.ConditionResult.INFO,
				ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		}

		performPostAuthorizationFlow();
	}

	protected void checkAccountRequestEndpointTLS() {
		eventLog.startBlock("Accounts request endpoint TLS test");
		env.mapKey("tls", "accounts_request_endpoint_tls");
		checkEndpointTLS();
		env.unmapKey("tls");
		eventLog.endBlock();
	}

	protected void checkAccountResourceEndpointTLS() {
		eventLog.startBlock("Accounts resource endpoint TLS test");
		env.mapKey("tls", "accounts_resource_endpoint_tls");
		checkEndpointTLS();
		env.unmapKey("tls");
		eventLog.endBlock();
	}

	protected void checkEndpointTLS() {
		callAndContinueOnFailure(EnsureTLS12.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-1");
	}

	protected void verifyAccessTokenWithResourceEndpoint() {
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-4");
		callAndStopOnFailure(SetPlainJsonAcceptHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "RFC7231-5.3.2");
		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);
		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");
		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);
	}

	protected void performPostAuthorizationFlow() {

		if (!isSecondClient()) {
			// call the token endpoint and complete the flow

			createAuthorizationCodeRequest();

			requestAuthorizationCode();

			checkAccountRequestEndpointTLS();

			checkAccountResourceEndpointTLS();

			requestProtectedResource();

			verifyAccessTokenWithResourceEndpoint();

			// Try the second client

			performAuthorizationFlowWithSecondClient();
		} else {
			// call the token endpoint and complete the flow

			createAuthorizationCodeRequest();

			requestAuthorizationCode();

			requestProtectedResource();

			switchToClient1AndTryClient2AccessToken();

			fireTestFinished();
		}
	}

	protected void performAuthorizationFlowWithSecondClient() {
		whichClient = 2;

		eventLog.startBlock(currentClientString() + "Setup");

		switchToSecondClient();

		Integer redirectQueryDisabled = env.getInteger("config", "disableRedirectQueryTest");

		if (redirectQueryDisabled != null && redirectQueryDisabled.intValue() != 0) {
			/* Temporary change to allow banks to disable tests until they have had a chance to register new
			 * clients with the new redirect uris.
			 */
			callAndContinueOnFailure(RedirectQueryTestDisabled.class, Condition.ConditionResult.FAILURE, "RFC6749-3.1.2");
		} else {
			callAndStopOnFailure(AddRedirectUriQuerySuffix.class, "RFC6749-3.1.2");
		}
		callAndStopOnFailure(CreateRedirectUri.class, "RFC6749-3.1.2");

		//exposeEnvString("client_id");

		performAuthorizationFlow();
	}

	protected void switchToClient1AndTryClient2AccessToken() {
		// Switch back to client 1
		eventLog.startBlock("Try Client1's MTLS client certificate with Client2's access token");
		unmapClient();

		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenExpectingError.class, Condition.ConditionResult.FAILURE, "OB-6.2.1-2");

		setStatus(Status.WAITING);
		eventLog.endBlock();
	}

	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		addClientAuthenticationToTokenEndpointRequest();
	}

	protected void addClientAuthenticationToTokenEndpointRequest() {
		call(sequence(addTokenEndpointClientAuthentication));
	}

	protected void requestAuthorizationCode() {

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-R-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

		// scope is not *required* to be returned as the request was passed in signed request object - FAPI-R-5.2.2-15
		// https://gitlab.com/openid/conformance-suite/issues/617

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			EnsureMinimumRefreshTokenLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			EnsureMinimumRefreshTokenEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2-24", "OIDCC-3.3.2.5");

		callAndContinueOnFailure(ValidateIdToken.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, ConditionResult.FAILURE, "OIDCC-2");

		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");

		performProfileIdTokenValidation();

		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-24");

		// This condition is a warning because we're not yet 100% sure of the code
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, ConditionResult.WARNING, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, ConditionResult.FAILURE,  "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "FAPI-RW-8.6");

		performTokenEndpointIdTokenExtraction();
		callAndContinueOnFailure(ExtractAtHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");

		/* these all use 'INFO' if the field isn't present - whether the hash is a may/should/shall is
		 * determined by the Extract*Hash condition
		 */
		skipIfMissing(new String[] { "c_hash" }, null, Condition.ConditionResult.INFO ,
			ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		skipIfMissing(new String[] { "s_hash" }, null, Condition.ConditionResult.INFO,
			ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");
		skipIfMissing(new String[] { "at_hash" }, null, Condition.ConditionResult.INFO,
			ValidateAtHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		if (!jarm) {
			eventLog.startBlock(currentClientString() + "Verify at_hash in the authorization endpoint id_token");

			env.mapKey("id_token", "authorization_endpoint_id_token");

			callAndContinueOnFailure(ExtractAtHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");

			skipIfMissing(new String[]{"at_hash"}, null, ConditionResult.INFO,
				ValidateAtHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			env.unmapKey("id_token");

			eventLog.endBlock();
		}
	}

	protected void processCallback() {

		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		if (jarm) {
			// FAPI-RW only allows jarm with the code flow and hence we extract the response from the url query
			callAndStopOnFailure(ExtractJARMFromURLQuery.class, "FAPI-RW-5.2.5","JARM-4.3.4","JARM-4.3.1");

			callAndContinueOnFailure(RejectNonJarmResponsesInUrlQuery.class, ConditionResult.FAILURE, "JARM-4.1");

			callAndStopOnFailure(ExtractAuthorizationEndpointResponseFromJARMResponse.class);

			callAndContinueOnFailure(ValidateJARMResponse.class, ConditionResult.FAILURE, "JARM-4.4-3", "JARM-4.4-4", "JARM-4.4-5");

			callAndContinueOnFailure(ValidateJARMExpRecommendations.class, ConditionResult.WARNING, "JARM-4.1");

			callAndContinueOnFailure(ValidateJARMSignatureUsingKid.class, ConditionResult.WARNING, "JARM-4.4-6");

		} else {
			// FAPI-RW otherwise always requires the hybrid flow, use the hash as the response
			env.mapKey("authorization_endpoint_response", "callback_params");
		}

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");

		onAuthorizationCallbackResponse();

		eventLog.endBlock();
	}

	protected void performProfileIdTokenValidation() {
		if (profileIdTokenValidationSteps != null) {
			call(sequence(profileIdTokenValidationSteps));
		}
	}

	protected void performTokenEndpointIdTokenExtraction() {
		/* code id_token flow - we already had an id_token from the authorisation endpoint,
		 * so c_hash and s_hash are optional.
		 */
		callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");
		callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.INFO, "FAPI-RW-5.2.2-4");
	}

	protected void requestProtectedResource() {

		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");

		if (!isSecondClient()) {
			callAndStopOnFailure(FAPIGenerateResourceEndpointRequestHeaders.class);
			// This header is no longer mentioned in the FAPI standard as of ID2, however the UK OB spec most banks are
			// using (v3.1.1) erroneously requires that this header is sent in all cases, so for now we send it in all cases
			// (even pure FAPI-RW, as it's hard to arrange otherwise).
			callAndStopOnFailure(AddFAPIFinancialIdToResourceEndpointRequest.class);
		}

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(EnsureResourceResponseContentTypeIsJsonUTF8.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-9", "FAPI-R-6.2.1-10");

		eventLog.endBlock();
	}

	protected boolean isSecondClient() {
		return whichClient == 2;
	}

	/** Return which client is in use, for use in block identifiers */
	protected String currentClientString() {
		if (isSecondClient()) {
			return "Second client: ";
		}
		return "";
	}

	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("mutual_tls_authentication");
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	@VariantSetup(parameter = FAPIProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		preAuthorizationSteps = null;
		profileAuthorizationEndpointSetupSteps = FAPIAuthorizationEndpointSetup.class;
		profileIdTokenValidationSteps = null;
	}

	@VariantSetup(parameter = FAPIProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		resourceConfiguration = OpenBankingUkResourceConfiguration.class;
		preAuthorizationSteps = () -> new OpenBankingUkPreAuthorizationSteps(isSecondClient(), addTokenEndpointClientAuthentication);
		profileAuthorizationEndpointSetupSteps = OpenBankingUkAuthorizationEndpointSetup.class;
		profileIdTokenValidationSteps = ValidateOpenBankingUkIdToken.class;
	}
}
