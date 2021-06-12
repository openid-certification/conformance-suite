package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.FAPIBrazilEncryptRequestObject;
import net.openid.conformance.condition.as.EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddCdrXvToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddClientIdToRequestObject;
import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddNbfToRequestObject;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddPlainErrorResponseAsAuthorizationEndpointResponseForJARM;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateExpiresIn;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectByValueRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectPostToPAREndpoint;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForPARResponseExpiresIn;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForRequestUriValue;
import net.openid.conformance.condition.client.CheckForSubjectInIdToken;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfPAREndpointResponseError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckServerKeysIsValid;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.EnsureIdTokenContainsKid;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenLength;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeLength;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenLength;
import net.openid.conformance.condition.client.EnsureMinimumRequestUriEntropy;
import net.openid.conformance.condition.client.EnsureResourceResponseReturnedJsonContentType;
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
import net.openid.conformance.condition.client.ExtractRequestUriFromPARResponse;
import net.openid.conformance.condition.client.ExtractSHash;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromOBResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.FAPIValidateEncryptedIdTokenHasKid;
import net.openid.conformance.condition.client.FAPIValidateIdTokenEncryptionAlg;
import net.openid.conformance.condition.client.FAPIValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.RejectNonJarmResponsesInUrlQuery;
import net.openid.conformance.condition.client.RejectStateInUrlQueryForHybridFlow;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToJWT;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToAccountsEndpoint;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.ValidateAtHash;
import net.openid.conformance.condition.client.ValidateCHash;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdToken;
import net.openid.conformance.condition.client.ValidateIdTokenACRClaimAgainstRequest;
import net.openid.conformance.condition.client.ValidateIdTokenEncrypted;
import net.openid.conformance.condition.client.ValidateIdTokenNonce;
import net.openid.conformance.condition.client.ValidateIdTokenSignature;
import net.openid.conformance.condition.client.ValidateIdTokenSignatureUsingKid;
import net.openid.conformance.condition.client.ValidateIssInAuthorizationResponse;
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
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.FAPIBrazilCheckKeyAlgInClientJWKs;
import net.openid.conformance.condition.common.FAPICheckKeyAlgInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToPAREndpointRequest;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.sequence.client.CDRAuthorizationEndpointSetup;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.sequence.client.FAPIAuthorizationEndpointSetup;
import net.openid.conformance.sequence.client.OpenBankingBrazilAuthorizationEndpointSetup;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.sequence.client.OpenBankingUkAuthorizationEndpointSetup;
import net.openid.conformance.sequence.client.OpenBankingUkPreAuthorizationSteps;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.sequence.client.ValidateOpenBankingUkIdToken;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

import java.util.function.Supplier;

@VariantParameters({
	ClientAuthType.class,
	FAPI1FinalOPProfile.class,
	FAPIResponseMode.class,
	FAPIAuthRequestMethod.class
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_uk", configurationFields = {
	"resource.resourceUrlAccountRequests",
	"resource.resourceUrlAccountsResource"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "consumerdataright_au", configurationFields = {
	"resource.cdrVersion"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"resource.consentUrl",
	"resource.brazilCpf"
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
public abstract class AbstractFAPI1AdvancedFinalServerTestModule extends AbstractRedirectServerTestModule {

	protected int whichClient;
	protected boolean jarm = false;
	protected boolean allowPlainErrorResponseForJarm = false;
	protected boolean isPar = false;

	// for variants to fill in by calling the setup... family of methods
	private Class <? extends ConditionSequence> resourceConfiguration;
	protected Class <? extends ConditionSequence> addTokenEndpointClientAuthentication;
	private Supplier <? extends ConditionSequence> preAuthorizationSteps;
	protected Class <? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;
	private Class <? extends ConditionSequence> profileIdTokenValidationSteps;
	private Class <? extends ConditionSequence> supportMTLSEndpointAliases;
	protected Class <? extends ConditionSequence> addParEndpointClientAuthentication;



	public static class FAPIResourceConfiguration extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		}
	}

	public static class OpenBankingUkResourceConfiguration extends AbstractConditionSequence {
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
		isPar = getVariant(FAPIAuthRequestMethod.class) == FAPIAuthRequestMethod.PUSHED;

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		if (supportMTLSEndpointAliases != null) {
			call(sequence(supportMTLSEndpointAliases));
		}

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);
		callAndContinueOnFailure(CheckServerKeysIsValid.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
		callAndContinueOnFailure(EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");
		callAndContinueOnFailure(FAPIEnsureMinimumServerKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-5.2.2-5", "FAPI1-BASE-5.2.2-6");

		whichClient = 1;

		// Set up the client configuration
		configureClient();

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		call(sequence(resourceConfiguration));

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
		callAndContinueOnFailure(ExtractTLSTestValuesFromOBResourceConfiguration.class, Condition.ConditionResult.INFO);

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
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndContinueOnFailure(FAPIBrazilCheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-1");
		} else {
			callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-8.6");
		}
		callAndContinueOnFailure(FAPIEnsureMinimumClientKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-5.2.2-5", "FAPI1-BASE-5.2.2-6");

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

		createAuthorizationRequestObject();

		if (isPar) {
			callAndStopOnFailure(BuildRequestObjectPostToPAREndpoint.class);
			addClientAuthenticationToPAREndpointRequest();
			performParAuthorizationRequestFlow();
		} else {
			buildRedirect();
			performRedirect();
		}
	}

	protected void buildRedirect() {
		callAndStopOnFailure(BuildRequestObjectByValueRedirectToAuthorizationEndpoint.class);
	}

	public static class CreateAuthorizationRequestSteps extends AbstractConditionSequence {

		private boolean isSecondClient;
		private boolean isJarm;
		private boolean isPar;
		private Class <? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;

		public CreateAuthorizationRequestSteps(boolean isSecondClient,
											   boolean isJarm,
											   boolean isPar,
											   Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps) {
			this.isSecondClient = isSecondClient;
			this.isJarm = isJarm;
			this.isPar = isPar;
			this.profileAuthorizationEndpointSetupSteps = profileAuthorizationEndpointSetupSteps;
		}

		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

			call(sequence(profileAuthorizationEndpointSetupSteps));

			if (isSecondClient) {
				exec().putInteger("requested_state_length", 128);
			} else {
				exec().putInteger("requested_state_length", null);
			}

			callAndStopOnFailure(CreateRandomStateValue.class);
			callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(CreateRandomNonceValue.class);
			callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

			if (isJarm) {
				callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
				callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToJWT.class);

			} else {
				callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
			}

			if (isPar) {
				call(new SetupPkceAndAddToAuthorizationRequest());
			}
		}

	}

	protected void createAuthorizationRequest() {
		call(makeCreateAuthorizationRequestSteps());
	}

	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return new CreateAuthorizationRequestSteps(isSecondClient(), jarm, isPar, profileAuthorizationEndpointSetupSteps);
	}

	public static class CreateAuthorizationRequestObjectSteps extends AbstractConditionSequence {

		protected boolean isSecondClient;
		protected boolean encrypt;

		public CreateAuthorizationRequestObjectSteps(boolean isSecondClient, boolean encrypt) {
			this.isSecondClient = isSecondClient;
			this.encrypt = encrypt;
		}

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			if (isSecondClient) {
				callAndStopOnFailure(AddIatToRequestObject.class);
			}
			callAndStopOnFailure(AddNbfToRequestObject.class, "FAPI1-ADV-5.2.2-17"); // mandatory in FAPI1-Advanced-Final
			callAndStopOnFailure(AddExpToRequestObject.class, "FAPI1-ADV-5.2.2-13");

			callAndStopOnFailure(AddAudToRequestObject.class, "FAPI1-ADV-5.2.2-14");

			// iss is a 'should' in OIDC & jwsreq,
			callAndStopOnFailure(AddIssToRequestObject.class, "OIDCC-6.1");

			// jwsreq-26 is very explicit that client_id should be both inside and outside the request object
			callAndStopOnFailure(AddClientIdToRequestObject.class, "FAPI1-ADV-5.2.3-8");

			callAndStopOnFailure(SignRequestObject.class);

			if (encrypt) {
				callAndStopOnFailure(FAPIBrazilEncryptRequestObject.class, "BrazilOB-5.2.2-1", "BrazilOB-6.1.1-1");
			}
		}
	}

	protected void createAuthorizationRequestObject() {
		call(makeCreateAuthorizationRequestObjectSteps());
	}

	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		boolean isPar = getVariant(FAPIAuthRequestMethod.class) == FAPIAuthRequestMethod.PUSHED;
		boolean isBrazil = getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.OPENBANKING_BRAZIL;
		boolean encrypt = isBrazil && !isPar;
		return new CreateAuthorizationRequestObjectSteps(isSecondClient(), encrypt);
	}

	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlQueryForHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		if (jarm) {
			callAndContinueOnFailure(ValidateSuccessfulJARMResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING);
		} else {
			callAndContinueOnFailure(ValidateSuccessfulHybridResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING);
		}

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		// as https://tools.ietf.org/html/draft-ietf-oauth-iss-auth-resp is still a draft we only warn if the value is wrong,
		// and do not require it to be present.
		callAndContinueOnFailure(ValidateIssInAuthorizationResponse.class, ConditionResult.WARNING, "OAuth2-iss-2");

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

		handleSuccessfulAuthorizationEndpointResponse();
	}

	// This is only used for the id token from the authorization endpoint, the token endpoint one is verified
	// separately (I'm not sure why)
	protected void performIdTokenValidation() {

		callAndContinueOnFailure(ValidateIdToken.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-4");

		callAndContinueOnFailure(EnsureIdTokenContainsKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, ConditionResult.FAILURE, "OIDCC-2");

		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");

		performProfileIdTokenValidation();

		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-4");

		// This condition is a warning because we're not yet 100% sure of the code
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, ConditionResult.WARNING, "FAPI1-ADV-5.2.2.1-4");

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, ConditionResult.FAILURE, "FAPI1-BASE-5.2.2.1-6", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "FAPI1-ADV-8.6");
		skipIfElementMissing("id_token", "jwe_header", ConditionResult.INFO,
			FAPIValidateIdTokenEncryptionAlg.class, ConditionResult.FAILURE,"FAPI1-ADV-8.6.1-1");
		skipIfElementMissing("id_token", "jwe_header", Condition.ConditionResult.INFO,
			FAPIValidateEncryptedIdTokenHasKid.class, Condition.ConditionResult.FAILURE,"OIDCC-10.1");
		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
			callAndContinueOnFailure(ValidateIdTokenEncrypted.class, ConditionResult.FAILURE, "CDR-tokens");
		}
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {

		if (!jarm) {
			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI1-ADV-5.2.2.1-4");

			// save the id_token returned from the authorization endpoint
			env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));
			performIdTokenValidation();

			callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");

			skipIfMissing(new String[]{"s_hash"}, null, Condition.ConditionResult.INFO,
				ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");

			callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			skipIfMissing(new String[]{"c_hash"}, null, Condition.ConditionResult.INFO,
				ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		}

		performPostAuthorizationFlow();
	}

	protected void performPostAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Call token endpoint");

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();
		requestAuthorizationCode();
		requestProtectedResource();
		onPostAuthorizationFlowComplete();
	}

	protected void onPostAuthorizationFlowComplete() {
		fireTestFinished();
	}

	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		addClientAuthenticationToTokenEndpointRequest();

		if (isPar) {
			addPkceCodeVerifier();
		}
	}

	protected void addPkceCodeVerifier() {
		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class, "RFC7636-4.5");
	}

	protected void addClientAuthenticationToTokenEndpointRequest() {
		call(sequence(addTokenEndpointClientAuthentication));
	}

	protected void addClientAuthenticationToPAREndpointRequest() {
		call(sequence(addParEndpointClientAuthentication));
	}

	protected void requestAuthorizationCode() {

		callAndStopOnFailure(CallTokenEndpoint.class);

		eventLog.startBlock(currentClientString() + "Verify token endpoint response");

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI1-BASE-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, "RFC6749-5.1");
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");
		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
				FAPIBrazilValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-13");
		}
		// scope is not *required* to be returned as the request was passed in signed request object - FAPI-R-5.2.2-15
		// https://gitlab.com/openid/conformance-suite/issues/617

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			EnsureMinimumRefreshTokenLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			EnsureMinimumRefreshTokenEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-5.2.2-16");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI1-BASE-5.2.2.1-6", "OIDCC-3.3.2.5");

		callAndContinueOnFailure(ValidateIdToken.class, ConditionResult.FAILURE, "FAPI1-BASE-5.2.2.1-6");

		callAndContinueOnFailure(EnsureIdTokenContainsKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, ConditionResult.FAILURE, "OIDCC-2");

		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");

		performProfileIdTokenValidation();

		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE, "FAPI1-BASE-5.2.2.1-6");

		// This condition is a warning because we're not yet 100% sure of the code
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, ConditionResult.WARNING, "FAPI1-BASE-5.2.2.1-6");

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, ConditionResult.FAILURE, "FAPI1-BASE-5.2.2.1-6", "OB-5.2.2-8");
		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndContinueOnFailure(FAPIBrazilValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "BrazilOB-6.1-1");
		} else {
			callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "FAPI1-ADV-8.6");
		}
		skipIfElementMissing("id_token", "jwe_header", ConditionResult.INFO,
			FAPIValidateIdTokenEncryptionAlg.class, ConditionResult.FAILURE,"FAPI1-ADV-8.6.1-1");
		skipIfElementMissing("id_token", "jwe_header", Condition.ConditionResult.INFO,
			FAPIValidateEncryptedIdTokenHasKid.class, Condition.ConditionResult.FAILURE,"OIDCC-10.1");
		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
			callAndContinueOnFailure(ValidateIdTokenEncrypted.class, ConditionResult.FAILURE, "CDR-tokens");
		}

		performTokenEndpointIdTokenExtraction();
		callAndContinueOnFailure(ExtractAtHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");

		/* these all use 'INFO' if the field isn't present - whether the hash is a may/should/shall is
		 * determined by the Extract*Hash condition
		 */
		skipIfMissing(new String[]{"c_hash"}, null, Condition.ConditionResult.INFO,
			ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		skipIfMissing(new String[]{"s_hash"}, null, Condition.ConditionResult.INFO,
			ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");
		skipIfMissing(new String[]{"at_hash"}, null, Condition.ConditionResult.INFO,
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
			processCallbackForJARM();
		} else {
			// FAPI-RW otherwise always requires the hybrid flow, use the hash as the response
			env.mapKey("authorization_endpoint_response", "callback_params");

			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");
		}
		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		onAuthorizationCallbackResponse();

		eventLog.endBlock();
	}

	/**
	 * For error responses, we allow a JARM response, or an error page or a plain (non-jarm) error response
	 * per https://gitlab.com/openid/conformance-suite/-/issues/860
	 */
	protected void processCallbackForJARM() {
		String errorParameter = env.getString("callback_query_params", "error");
		String responseParameter = env.getString("callback_query_params", "response");
		if(allowPlainErrorResponseForJarm && responseParameter==null && errorParameter!=null) {
			//plain error response, no jarm
			callAndStopOnFailure(AddPlainErrorResponseAsAuthorizationEndpointResponseForJARM.class);
		} else {
			// FAPI-RW only allows jarm with the code flow and hence we extract the response from the url query
			callAndStopOnFailure(ExtractJARMFromURLQuery.class, "FAPI1-ADV-5.2.3.2-1", "JARM-4.3.4", "JARM-4.3.1");

			callAndContinueOnFailure(RejectNonJarmResponsesInUrlQuery.class, ConditionResult.FAILURE, "JARM-4.1");

			callAndStopOnFailure(ExtractAuthorizationEndpointResponseFromJARMResponse.class);

			callAndContinueOnFailure(ValidateJARMResponse.class, ConditionResult.FAILURE, "JARM-4.4-3", "JARM-4.4-4", "JARM-4.4-5");

			callAndContinueOnFailure(ValidateJARMExpRecommendations.class, ConditionResult.WARNING, "JARM-4.1");

			callAndContinueOnFailure(ValidateJARMSignatureUsingKid.class, ConditionResult.WARNING, "JARM-4.4-6");
		}
	}


	protected void performProfileIdTokenValidation() {
		if (profileIdTokenValidationSteps != null) {
			call(sequence(profileIdTokenValidationSteps));
		}
	}

	protected void performTokenEndpointIdTokenExtraction() {
		/* code id_token flow - we already had an id_token from the authorization endpoint,
		 * so c_hash and s_hash are optional.
		 */
		callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");
		callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.INFO, "FAPI1-ADV-5.2.2.1-5");
	}

	protected void requestProtectedResource() {

		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		if (isSecondClient()) {
			if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
				// CDR requires this header for all authenticated resource server endpoints
				callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3", "CDR-http-headers");
			}
		} else {
			// these are optional; only add them for the first client
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");

			callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");
			if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
				// CDR requires this header when the x-fapi-customer-ip-address header is present
				callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
			}

			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");
		}

		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
			callAndStopOnFailure(AddCdrXvToResourceEndpointRequest.class, "CDR-http-headers");
		}

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		if (!isSecondClient()) {
			callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");
		}

		callAndContinueOnFailure(EnsureResourceResponseReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-9", "FAPI1-BASE-6.2.1-10");

		eventLog.endBlock();
	}

	protected boolean isSecondClient() {
		return whichClient == 2;
	}

	/**
	 * Return which client is in use, for use in block identifiers
	 */
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
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		addParEndpointClientAuthentication = AddMTLSClientAuthenticationToPAREndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
		// FAPI requires the use of MTLS sender constrained access tokens, so we must use the MTLS version of the
		// token endpoint even when using private_key_jwt client authentication
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		addParEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		preAuthorizationSteps = null;
		profileAuthorizationEndpointSetupSteps = FAPIAuthorizationEndpointSetup.class;
		profileIdTokenValidationSteps = null;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		resourceConfiguration = OpenBankingUkResourceConfiguration.class;
		preAuthorizationSteps = () -> new OpenBankingUkPreAuthorizationSteps(isSecondClient(), false, addTokenEndpointClientAuthentication);
		profileAuthorizationEndpointSetupSteps = OpenBankingUkAuthorizationEndpointSetup.class;
		profileIdTokenValidationSteps = ValidateOpenBankingUkIdToken.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "consumerdataright_au")
	public void setupConsumerDataRightAu() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		preAuthorizationSteps = null;
		profileAuthorizationEndpointSetupSteps = CDRAuthorizationEndpointSetup.class;
		profileIdTokenValidationSteps = null;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		preAuthorizationSteps = () -> new OpenBankingBrazilPreAuthorizationSteps(isSecondClient(), addTokenEndpointClientAuthentication);
		profileAuthorizationEndpointSetupSteps = OpenBankingBrazilAuthorizationEndpointSetup.class;
		profileIdTokenValidationSteps = null;
	}

	protected void performPARRedirectWithRequestUri() {
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class, "PAR-4");
		performRedirect();
	}

	protected void performParAuthorizationRequestFlow() {

		callAndStopOnFailure(CallPAREndpoint.class, "PAR-2.1");

		processParResponse();
	}

	protected void processParResponse() {
		callAndStopOnFailure(CheckIfPAREndpointResponseError.class, "PAR-2.2", "PAR-2.3");

		callAndStopOnFailure(CheckForRequestUriValue.class, "PAR-2.2");

		callAndContinueOnFailure(CheckForPARResponseExpiresIn.class, ConditionResult.FAILURE, "PAR-2.2");

		callAndStopOnFailure(ExtractRequestUriFromPARResponse.class);

		callAndContinueOnFailure(EnsureMinimumRequestUriEntropy.class, ConditionResult.FAILURE, "PAR-2.2", "PAR-7.1", "JAR-10.2");

		performPARRedirectWithRequestUri();
	}
}
