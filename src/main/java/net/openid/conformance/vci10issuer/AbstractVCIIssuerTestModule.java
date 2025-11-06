package net.openid.conformance.vci10issuer;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.FAPIBrazilEncryptRequestObject;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddClientIdToRequestObject;
import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.AddNbfToRequestObject;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.BuildRequestObjectByValueRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectPostToPAREndpoint;
import net.openid.conformance.condition.client.BuildUnsignedPAREndpointRequest;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CallPAREndpointAllowingDpopNonceError;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallProtectedResourceAllowingDpopNonceError;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForPARResponseExpiresIn;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForRequestUriValue;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckPAREndpointResponse201WithNoError;
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
import net.openid.conformance.condition.client.EnsureHttpStatusCode;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureIdTokenContainsKid;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenLength;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeLength;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenLength;
import net.openid.conformance.condition.client.EnsureMinimumRequestUriEntropy;
import net.openid.conformance.condition.client.ExpectNoIdTokenInTokenResponse;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAtHash;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractCHash;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ExtractRequestUriFromPARResponse;
import net.openid.conformance.condition.client.ExtractSHash;
import net.openid.conformance.condition.client.FAPI2ValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentInitiationRequest;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.ParseCredentialAsSdJwt;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlFragment;
import net.openid.conformance.condition.client.RejectErrorInUrlFragment;
import net.openid.conformance.condition.client.RejectStateInUrlFragmentForCodeFlow;
import net.openid.conformance.condition.client.RequireIssInAuthorizationResponse;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToAccountsEndpoint;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.SignRequestObjectIncludeMediaType;
import net.openid.conformance.condition.client.ValidateAtHash;
import net.openid.conformance.condition.client.ValidateCHash;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateClientPrivateKeysAreDifferent;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateCredentialJWTVct;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdTokenFromTokenResponseEncryption;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.client.ValidateSHash;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.client.ValidateSuccessfulAuthCodeFlowResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.FAPI2CheckKeyAlgInClientJWKs;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.openid.federation.CallCredentialIssuerNonceEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToPAREndpointRequest;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest;
import net.openid.conformance.sequence.client.PerformStandardIdTokenChecks;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VCIServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import net.openid.conformance.vci10issuer.condition.VCICheckCacheControlHeaderInResponse;
import net.openid.conformance.vci10issuer.condition.VCICheckExpClaimInCredential;
import net.openid.conformance.vci10issuer.condition.VCICreateCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateTokenEndpointRequestForPreAuthorizedCodeGrant;
import net.openid.conformance.vci10issuer.condition.VCIDetermineCredentialConfigurationTransferMethod;
import net.openid.conformance.vci10issuer.condition.VCIExtractCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractPreAuthorizedCodeAndTxCodeFromCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIExtractTxCodeFromRequest;
import net.openid.conformance.vci10issuer.condition.VCIFetchCredentialIssuerMetadataSequence;
import net.openid.conformance.vci10issuer.condition.VCIFetchCredentialOfferFromCredentialOfferUri;
import net.openid.conformance.vci10issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIGenerateKeyAttestationJwt;
import net.openid.conformance.vci10issuer.condition.VCIGenerateProofJwt;
import net.openid.conformance.vci10issuer.condition.VCIGenerateRichAuthorizationRequestForCredential;
import net.openid.conformance.vci10issuer.condition.VCIResolveCredentialEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCISelectOAuthorizationServer;
import net.openid.conformance.vci10issuer.condition.VCITryAddingIssuerStateToAuthorizationRequest;
import net.openid.conformance.vci10issuer.condition.VCITryToExtractIssuerStateFromCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIUseStaticTxCodeFromConfig;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialNonceResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialOfferRequestParams;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialValidityInfoIsPresent;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIEnsureX5cHeaderPresentForSdJwtCredential;
import net.openid.conformance.vci10issuer.condition.VCIWaitForCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIWaitForTxCode;
import net.openid.conformance.vci10issuer.condition.clientattestation.AddClientAttestationClientAuthToEndpointRequest;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationJwt;
import net.openid.conformance.vci10issuer.condition.clientattestation.GenerateClientAttestationClientInstanceKey;
import net.openid.conformance.vci10issuer.condition.statuslist.VCIValidateCredentialValidityByStatusListIfPresent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@VariantParameters({
	VCIClientAuthType.class,
	FAPI2AuthRequestMethod.class,
	FAPI2SenderConstrainMethod.class,
	AuthorizationRequestType.class,
	VCIProfile.class,
	VCIGrantType.class,
	VCIAuthorizationCodeFlowVariant.class,
})
@VariantHidesConfigurationFields(parameter = VCIAuthorizationCodeFlowVariant.class, value="wallet_initiated", configurationFields = {
	"vci.credential_offer_endpoint"
})
@VariantHidesConfigurationFields(parameter = VCIClientAuthType.class, value="private_key_jwt", configurationFields = {
	"vci.client_attestation_issuer"
})
@VariantHidesConfigurationFields(parameter = VCIClientAuthType.class, value="mtls", configurationFields = {
	"vci.client_attestation_issuer"
})
@VariantConfigurationFields(parameter = FAPI2SenderConstrainMethod.class, value = "dpop", configurationFields = {"client.dpop_signing_alg", "client2.dpop_signing_alg",})
@VariantConfigurationFields(parameter = VCIClientAuthType.class, value = "client_attestation", configurationFields = {
	"vci.client_attester_keys_jwks",
	"vci.client_attestation_issuer"
})
@VariantConfigurationFields(parameter = VCIGrantType.class, value = "pre_authorization_code", configurationFields = {"vci.static_tx_code"})
@VariantConfigurationFields(parameter = VCIClientAuthType.class, value="mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca",
})
public abstract class AbstractVCIIssuerTestModule extends AbstractRedirectServerTestModule {

	protected int whichClient;
	protected boolean allowPlainErrorResponseForJarm = false;
	protected Boolean isPar;
	protected Boolean isOpenId;
	protected Boolean isSignedRequest;
	protected Boolean profileRequiresMtlsEverywhere;
	protected Boolean useDpopAuthCodeBinding;
	protected Boolean isRarRequest;

	protected VCIClientAuthType clientAuthType;

	protected VCIProfile vciProfile;

	protected VCIGrantType vciGrantType;
	protected VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant;

	// for variants to fill in by calling the setup... family of methods
	private Class<? extends ConditionSequence> resourceConfiguration;
	protected Class<? extends ConditionSequence> addTokenEndpointClientAuthentication;
	private Supplier<? extends ConditionSequence> preAuthorizationSteps;
	protected Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;
	private Class<? extends ConditionSequence> profileIdTokenValidationSteps;
	private Class<? extends ConditionSequence> supportMTLSEndpointAliases;
	protected Class<? extends ConditionSequence> addParEndpointClientAuthentication;
	protected Supplier<? extends ConditionSequence> createDpopForParEndpointSteps;
	protected Supplier<? extends ConditionSequence> createDpopForTokenEndpointSteps;
	protected Supplier<? extends ConditionSequence> createDpopForResourceEndpointSteps;

	protected Supplier<? extends ConditionSequence> fetchCredentialIssuerMetadataSteps;

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

	protected Boolean isDpop() {
		return getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.DPOP;
	}

	protected Boolean isMTLS() {
		return getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS;
	}

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
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

		isPar = true;
		isOpenId = false;
		isSignedRequest = getVariant(FAPI2AuthRequestMethod.class) == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION;
		isRarRequest = getVariant(AuthorizationRequestType.class) == AuthorizationRequestType.RAR;
		useDpopAuthCodeBinding = false;

		clientAuthType = getVariant(VCIClientAuthType.class);
		vciProfile = getVariant(VCIProfile.class);

		vciGrantType = getVariant(VCIGrantType.class);
		vciAuthorizationCodeFlowVariant = getVariant(VCIAuthorizationCodeFlowVariant.class);

		// FAPI2ID2OPProfile.PLAIN_FAPI configuration
		setupPlainFapi();

		// https://gitlab.com/idmvp/specifications/-/issues/29
		profileRequiresMtlsEverywhere = false;


		fetchCredentialIssuerMetadataSteps = () -> new VCIFetchCredentialIssuerMetadataSequence(VCIServerMetadata.DISCOVERY);

		eventLog.runBlock("Fetch Credential Issuer Metadata", this::fetchCredentialIssuerMetadata);

		eventLog.startBlock("Fetch Authorization Server Metadata");

		callAndStopOnFailure(VCIFetchOAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3", "RFC8414-3.1");

		callAndStopOnFailure(VCISelectOAuthorizationServer.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3");

		eventLog.endBlock();

		eventLog.startBlock("Configure Test");

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		if (supportMTLSEndpointAliases != null) {
			call(sequence(supportMTLSEndpointAliases));
		}

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		determineCredentialConfigurationTransferMethod();

		if (isOpenId) {
			callAndStopOnFailure(FetchServerKeys.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckServerKeysIsValid.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
			callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
			callAndContinueOnFailure(EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");
			callAndContinueOnFailure(FAPIEnsureMinimumServerKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4-2", "FAPI2-SP-ID2-5.4-3");
		}

		whichClient = 1;

		// Set up the client configuration
		eventLog.startBlock("Configure Client");
		configureClient();
		eventLog.endBlock();

		eventLog.startBlock("Configure Resource Endpoint");
		setupResourceEndpoint();
		eventLog.endBlock();

		if (clientAuthType == VCIClientAuthType.CLIENT_ATTESTATION) {
			eventLog.startBlock("Configure Client Attestation");
			generateClientAttestationKeys();
			eventLog.endBlock();
		}

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void determineCredentialConfigurationTransferMethod() {

		String vciCredentialConfigurationId = env.getString("config", "vci.credential_configuration_id");
		if (vciCredentialConfigurationId == null || vciCredentialConfigurationId.isBlank()) {
			throw new TestFailureException(getId(), "credential_configuration_id cannot be null or empty!");
		}
		exposeEnvString("credential_configuration_id", "config", "vci.credential_configuration_id");

		callAndStopOnFailure(VCIDetermineCredentialConfigurationTransferMethod.class,  ConditionResult.FAILURE);
	}

	protected void setupResourceEndpoint() {
		// Set up the resource endpoint configuration
		callAndStopOnFailure(VCIResolveCredentialEndpointToUse.class);
		call(sequence(resourceConfiguration));
	}

	protected void onConfigure(JsonObject config, String baseUrl) {

		// No custom configuration
	}

	protected void configureClient() {
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		boolean mtlsRequired = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS || clientAuthType == VCIClientAuthType.MTLS || profileRequiresMtlsEverywhere;

		if (mtlsRequired) {
			callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
		}

		validateClientConfiguration();
	}

	protected void configureSecondClient() {
		eventLog.startBlock("Verify configuration of second client");

		switchToSecondClient();
		callAndStopOnFailure(GetStaticClient2Configuration.class);

		boolean mtlsRequired = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS || clientAuthType == VCIClientAuthType.MTLS || profileRequiresMtlsEverywhere;

		if (mtlsRequired) {
			callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);
		}

		validateClientConfiguration();

		unmapClient();

		callAndContinueOnFailure(ValidateClientPrivateKeysAreDifferent.class, ConditionResult.FAILURE);

		eventLog.endBlock();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		call(exec().mapKey("client_request", requestId));

		try {
			if ("credential_offer".equals(path)) {
				return handleCredentialOffer(req, res, session, requestParts);
			}

			if ("tx_code".equals(path)) {
				return handleTxCode();
			}

			return super.handleHttp(path, req, res, session, requestParts);
		} finally {
			call(exec().unmapKey("client_request"));
		}
	}

	protected Object handleTxCode() {

		setStatus(Status.RUNNING);
		callAndStopOnFailure(VCIExtractTxCodeFromRequest.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5");
		performPreAuthorizationCodeFlow();

		return new ModelAndView("resultCaptured",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	protected Object handleCredentialOffer(HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		switch (vciGrantType) {
			case AUTHORIZATION_CODE -> {

				processCredentialOffer(requestParts);

				performAuthorizationFlow();
			}
			case PRE_AUTHORIZATION_CODE -> {

				processCredentialOffer(requestParts);

				if (env.getElementFromObject("config", "vci.static_tx_code") != null) {
					callAndStopOnFailure(VCIUseStaticTxCodeFromConfig.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5");

					performPreAuthorizationCodeFlow();
				} else {
					waitForTxCode();
				}
			}
		}

		return new ModelAndView("resultCaptured",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	protected void waitForTxCode() {
		expose("tx_code_endpoint", env.getString("base_url") + "/tx_code?code=your_tx_code");
		callAndStopOnFailure(VCIWaitForTxCode.class,ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5");

		setStatus(Status.WAITING);
		// performPreAuthorizationCodeFlow() is called in handleTxCode()
	}

	protected void processCredentialOffer(JsonObject requestParts) {
		JsonObject queryStringParams = requestParts.get("query_string_params").getAsJsonObject();
		callAndStopOnFailure(new VCIValidateCredentialOfferRequestParams(requestParts), ConditionResult.FAILURE, "OID4VCI-1FINAL-4.1");

		if (queryStringParams.has("credential_offer_uri")) {
			callAndStopOnFailure(VCIFetchCredentialOfferFromCredentialOfferUri.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-4.1.3");
		}

		callAndStopOnFailure(VCIValidateCredentialOffer.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-4.1");

		if (vciGrantType == VCIGrantType.AUTHORIZATION_CODE) {
			callAndStopOnFailure(VCITryToExtractIssuerStateFromCredentialOffer.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-4.1.1");
			// TODO add support for deriving authorization_server to use from credential offer,
			//  see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-4.1.1-5.1.2.2
		}
	}

	protected void validateClientConfiguration() {
		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(FAPI2CheckKeyAlgInClientJWKs.class, ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4");
		callAndContinueOnFailure(FAPIEnsureMinimumClientKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4-2", "FAPI2-SP-ID2-5.4-3");

		boolean mtlsRequired = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS || clientAuthType == VCIClientAuthType.MTLS || profileRequiresMtlsEverywhere;

		if (mtlsRequired) {
			callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);
		}
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);
		switch (vciAuthorizationCodeFlowVariant) {

			case WALLET_INITIATED -> {
				switch (vciGrantType) {
					case AUTHORIZATION_CODE -> performAuthorizationFlow();
					case PRE_AUTHORIZATION_CODE -> throw new UnsupportedOperationException("Pre-authorization code is not supported for wallet initiated flow");
				}
			}

			case ISSUER_INITIATED -> {
				waitForCredentialOffer();
			}
		}
	}

	protected void waitForCredentialOffer() {
		expose("credential_offer_endpoint", env.getString("base_url") + "/credential_offer");
		callAndStopOnFailure(VCIWaitForCredentialOffer.class,  ConditionResult.FAILURE, "OID4VCI-1FINAL-4.1");
		setStatus(Status.WAITING);
	}

	protected void performPreAuthorizationCodeFlow() {

		callAndStopOnFailure(VCIExtractPreAuthorizedCodeAndTxCodeFromCredentialOffer.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5", "OID4VCI-1FINAL-4.1.1");

		performPostAuthorizationFlow();
	}

	protected void fetchCredentialIssuerMetadata() {
		call(sequence(fetchCredentialIssuerMetadataSteps));
	}

	protected void performPreAuthorizationSteps() {
		if (preAuthorizationSteps != null) {
			call(sequence(preAuthorizationSteps));
		}
	}

	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Create authorization request");

		createAuthorizationRequest();

		if (isSignedRequest) {
			createAuthorizationRequestObject();
		} else {
			// the request object is implicitly created by the PAR endpoint, but
			// AbstractBuildRequestObjectRedirectToAuthorizationEndpoint needs to know what is in the implicit
			// request object.
			env.mapKey("request_object_claims", "pushed_authorization_request_form_parameters");
		}

		if (isPar) {
			eventLog.startBlock(currentClientString() + "Make request to PAR endpoint");
			if (isSignedRequest) {
				callAndStopOnFailure(BuildRequestObjectPostToPAREndpoint.class);
			} else {
				callAndStopOnFailure(BuildUnsignedPAREndpointRequest.class);
			}

			if (env.getObject("pushed_authorization_request_endpoint_request_headers") == null) {
				env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());
			}
			env.mapKey("request_headers", "pushed_authorization_request_endpoint_request_headers");
			addClientAuthenticationToPAREndpointRequest();
			performParAuthorizationRequestFlow();
			env.unmapKey("request_headers");
		} else {
			eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
			buildRedirect();
			performRedirect();
		}
	}

	protected void buildRedirect() {
		callAndStopOnFailure(BuildRequestObjectByValueRedirectToAuthorizationEndpoint.class);
	}

	private static class CreateAuthorizationRequestSteps extends AbstractConditionSequence {

		private boolean isSecondClient;
		private boolean isOpenId;
		private boolean usePkce;
		private Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;
		private final VCIGrantType vciGrantType;
		private final VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant;

		public CreateAuthorizationRequestSteps(boolean isSecondClient, boolean isOpenId, boolean usePkce, Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps, VCIGrantType vciGrantType, VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant) {
			this.isSecondClient = isSecondClient;
			this.isOpenId = isOpenId;
			// it would probably be preferable to use the 'skip' syntax instead of the 'usePkce' flag, but it's
			// currently not possible to use 'skip' to skip a conditionsequence within a condition sequence
			this.usePkce = usePkce;
			this.profileAuthorizationEndpointSetupSteps = profileAuthorizationEndpointSetupSteps;
			this.vciGrantType = vciGrantType;
			this.vciAuthorizationCodeFlowVariant = vciAuthorizationCodeFlowVariant;
		}

		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

			if (profileAuthorizationEndpointSetupSteps != null) {
				call(sequence(profileAuthorizationEndpointSetupSteps));
			}

			if (isSecondClient) {
				call(exec().putInteger("requested_state_length", 128));
			} else {
				call(exec().removeNativeValue("requested_state_length"));
			}

			callAndStopOnFailure(CreateRandomStateValue.class);
			callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

			if (isOpenId) {
				callAndStopOnFailure(CreateRandomNonceValue.class);
				callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);
			}

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);

			if (usePkce) {
				call(new SetupPkceAndAddToAuthorizationRequest());
			}

			if (vciGrantType == VCIGrantType.AUTHORIZATION_CODE) {
				if (vciAuthorizationCodeFlowVariant == VCIAuthorizationCodeFlowVariant.ISSUER_INITIATED) {
					callAndStopOnFailure(VCITryAddingIssuerStateToAuthorizationRequest.class);
				}
			}
		}

	}

	protected void createAuthorizationRequest() {
		call(makeCreateAuthorizationRequestSteps());
	}

	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		ConditionSequence seq = new CreateAuthorizationRequestSteps(isSecondClient(), isOpenId, usePkce, profileAuthorizationEndpointSetupSteps, vciGrantType, vciAuthorizationCodeFlowVariant);
		if (isRarRequest) {
			seq.then(condition(VCIGenerateRichAuthorizationRequestForCredential.class).onFail(ConditionResult.FAILURE).requirements("OID4VCI-1FINAL-5.1.1"));
			seq.then(condition(RARSupport.AddRARToAuthorizationEndpointRequest.class));
		}
		return seq;
	}

	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return makeCreateAuthorizationRequestSteps(true);
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
			callAndStopOnFailure(AddNbfToRequestObject.class, "FAPI2-MS-ID1-5.3.1-3"); // mandatory in FAPI2-Message-Signing-ID1
			callAndStopOnFailure(AddExpToRequestObject.class, "FAPI2-MS-ID1-5.3.1-4");

			callAndStopOnFailure(AddAudToRequestObject.class, "FAPI2-SP-ID2-5.3.1.1-6");

			// iss is a 'should' in OIDC & jwsreq,
			callAndStopOnFailure(AddIssToRequestObject.class, "OIDCC-6.1");

			// jwsreq-26 is very explicit that client_id should be both inside and outside the request object
			callAndStopOnFailure(AddClientIdToRequestObject.class, "JAR-5", "FAPI2-MS-ID1-5.3.2-1");

			if (isSecondClient) {
				callAndStopOnFailure(SignRequestObjectIncludeMediaType.class, "JAR-4");
			} else {
				callAndStopOnFailure(SignRequestObject.class);
			}

			if (encrypt) {
				callAndStopOnFailure(FAPIBrazilEncryptRequestObject.class, "BrazilOB-5.2.2-1", "BrazilOB-6.1.2");
			}
		}
	}

	protected void createAuthorizationRequestObject() {
		call(makeCreateAuthorizationRequestObjectSteps());
	}

	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		boolean encrypt = false;
		return new CreateAuthorizationRequestObjectSteps(isSecondClient(), encrypt);
	}

	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlFragmentForCodeFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		callAndContinueOnFailure(ValidateSuccessfulAuthCodeFlowResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING);

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE, "OIDCC-3.2.2.5");

		callAndContinueOnFailure(RequireIssInAuthorizationResponse.class, ConditionResult.FAILURE, "OAuth2-iss-2", "FAPI2-SP-ID2-5.3.1.2-7");

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

		handleSuccessfulAuthorizationEndpointResponse();
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {
		performPostAuthorizationFlow();
	}

	protected void performPostAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Call token endpoint");

		// call the token endpoint and complete the flow
		switch(vciGrantType) {
			case AUTHORIZATION_CODE -> createAuthorizationCodeRequest();
			case PRE_AUTHORIZATION_CODE -> createPreAuthorizationCodeRequest();
		}

		exchangeAuthorizationCode();
		requestProtectedResource();
		onPostAuthorizationFlowComplete();
	}

	protected void onPostAuthorizationFlowComplete() {
		fireTestFinished();
	}

	protected void createPreAuthorizationCodeRequest() {

		callAndStopOnFailure(VCICreateTokenEndpointRequestForPreAuthorizedCodeGrant.class);
	}

	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		if (env.getObject("token_endpoint_request_headers") == null) {
			env.putObject("token_endpoint_request_headers", new JsonObject());
		}

		addPkceCodeVerifier();
	}

	protected void addPkceCodeVerifier() {
		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class, "RFC7636-4.5", "FAPI2-SP-ID2-5.3.2.2-3");
	}

	protected void addClientAuthenticationToTokenEndpointRequest() {
		env.mapKey("request_headers", "token_endpoint_request_headers");
		call(sequence(addTokenEndpointClientAuthentication));
		env.unmapKey("request_headers");
	}

	protected void addClientAuthenticationToPAREndpointRequest() {
		call(sequence(addParEndpointClientAuthentication));
	}


	/**
	 * Call sender constrained token endpoint. For DPOP nonce errors, it will retry with new server nonce value.
	 *
	 * @param fullResponse whether the full response should be returned
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse)
	 */
	protected void callSenderConstrainedTokenEndpointAndStopOnFailure(boolean fullResponse, String... requirements) {
		final int MAX_RETRY = 2;

		if (isDpop()) {
			int i = 0;
			while (i < MAX_RETRY) {
				addClientAuthenticationToTokenEndpointRequest();
				createDpopForTokenEndpoint();
				callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class, requirements);
				if (Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		} else {
			addClientAuthenticationToTokenEndpointRequest();
			callAndStopOnFailure(fullResponse ? CallTokenEndpointAndReturnFullResponse.class : CallTokenEndpoint.class, requirements);
		}
	}

	/**
	 * Call sender constrained token endpoint returning full response
	 *
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse)
	 */
	protected void callSenderConstrainedTokenEndpointAndStopOnFailure(String... requirements) {
		callSenderConstrainedTokenEndpointAndStopOnFailure(true, requirements);
	}

	/**
	 * Default Call to sender constrained token endpoint with non-full response
	 */
	protected void callSenderConstrainedTokenEndpoint() {
		callSenderConstrainedTokenEndpointAndStopOnFailure(false);
	}

	protected void exchangeAuthorizationCode() {
		callSenderConstrainedTokenEndpoint();

		eventLog.startBlock(currentClientString() + "Verify token endpoint response");
		processTokenEndpointResponse();
		eventLog.endBlock();
	}

	protected void processTokenEndpointResponse() {
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "RFC6749-4.1.4");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, ConditionResult.WARNING, "RFC6749-5.1");
		skipIfMissing(new String[]{"expires_in"}, null, Condition.ConditionResult.INFO, ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");
		// scope is not *required* to be returned as the request was passed in signed request object - FAPI-R-5.2.2-15
		// https://gitlab.com/openid/conformance-suite/issues/617

		callAndContinueOnFailure(CheckForRefreshTokenValue.class, ConditionResult.INFO);

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO, EnsureMinimumRefreshTokenLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO, EnsureMinimumRefreshTokenEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4-4");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4-4");

		if (isOpenId) {
			skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO, ValidateIdTokenFromTokenResponseEncryption.class, Condition.ConditionResult.WARNING, "OIDCC-10.2");
			callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI2-SP-ID2-5.3.1.3", "OIDCC-3.3.2.5");

			call(new PerformStandardIdTokenChecks());

			callAndContinueOnFailure(EnsureIdTokenContainsKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

			performProfileIdTokenValidation();

			callAndContinueOnFailure(FAPI2ValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4");

			// code flow - all hashes are optional.
			callAndContinueOnFailure(ExtractCHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");
			callAndContinueOnFailure(ExtractSHash.class, ConditionResult.INFO, "FAPI1-ADV-5.2.2.1-5");
			callAndContinueOnFailure(ExtractAtHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");

			/* these all use 'INFO' if the field isn't present - whether the hash is a may/should/shall is
			 * determined by the Extract*Hash condition
			 */
			skipIfMissing(new String[]{"c_hash"}, null, Condition.ConditionResult.INFO, ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
			skipIfMissing(new String[]{"s_hash"}, null, Condition.ConditionResult.INFO, ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");
			skipIfMissing(new String[]{"at_hash"}, null, Condition.ConditionResult.INFO, ValidateAtHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		} else {
			callAndStopOnFailure(ExpectNoIdTokenInTokenResponse.class);
		}

		if (isRarRequest) {
			callAndStopOnFailure(RARSupport.CheckForAuthorizationDetailsInTokenResponse.class, "RAR-7");
		}
	}

	protected void createDpopForTokenEndpoint() {
		if (null == env.getElementFromObject("client", "dpop_private_jwk")) {
			callAndStopOnFailure(GenerateDpopKey.class);
		}
		if (null != createDpopForTokenEndpointSteps) {
			call(sequence(createDpopForTokenEndpointSteps));
		}
	}

	protected void createDpopForParEndpoint() {

		if (null == env.getElementFromObject("client", "dpop_private_jwk")) {
			callAndStopOnFailure(GenerateDpopKey.class);
		}
		if (null != createDpopForParEndpointSteps) {
			call(sequence(createDpopForParEndpointSteps));
		}
	}

	@Override
	protected void processCallback() {

		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		// FAPI2 always requires the auth code flow, use the query as the response
		env.mapKey("authorization_endpoint_response", "callback_query_params");

		callAndContinueOnFailure(RejectErrorInUrlFragment.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");

		callAndContinueOnFailure(RejectAuthCodeInUrlFragment.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		onAuthorizationCallbackResponse();

		eventLog.endBlock();
	}

	protected void performProfileIdTokenValidation() {
		if (profileIdTokenValidationSteps != null) {
			call(sequence(profileIdTokenValidationSteps));
		}
	}

	public static class UpdateResourceRequestSteps extends AbstractConditionSequence {

		protected Supplier<? extends ConditionSequence> createDpopForResourceEndpointSteps;
		protected boolean brazilPayments;

		public UpdateResourceRequestSteps(Supplier<? extends ConditionSequence> createDpopForResourceEndpointSteps, boolean brazilPayments) {
			this.createDpopForResourceEndpointSteps = createDpopForResourceEndpointSteps;
			this.brazilPayments = brazilPayments;
		}

		@Override
		public void evaluate() {
			if (createDpopForResourceEndpointSteps != null) {
				call(sequence(createDpopForResourceEndpointSteps));
			}
			if (brazilPayments) {
				// we use the idempotency header to allow us to make a request more than once; however it is required
				// that a new jwt is sent in each retry, so update jti/iat & resign
				call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));
				callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
				callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");
				call(exec().unmapKey("request_object_claims"));
				callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
			}
		}
	}

	protected ConditionSequence makeUpdateResourceRequestSteps() {
		return new UpdateResourceRequestSteps(createDpopForResourceEndpointSteps, false);
	}

	// Make any necessary updates to a resource request before we send it again
	protected void updateResourceRequest() {
		call(makeUpdateResourceRequestSteps());
	}

	protected void updateResourceRequestAndCallProtectedResourceUsingDpop(String... requirements) {
		if (isDpop()) {
			final int MAX_RETRY = 2;
			int i = 0;
			while (i < MAX_RETRY) {
				updateResourceRequest();
				callAndStopOnFailure(CallProtectedResourceAllowingDpopNonceError.class, requirements);
				if (Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break; // no nonce error so
				}
				// continue call with nonce
				++i;
			}
		}
	}

	protected void requestProtectedResourceUsingDpop() {
		if (isDpop() && (createDpopForResourceEndpointSteps != null)) {
			final int MAX_RETRY = 2;
			int i = 0;
			while (i < MAX_RETRY) {

				call(sequence(createDpopForResourceEndpointSteps));
				callAndStopOnFailure(CallProtectedResourceAllowingDpopNonceError.class, "OID4VCI-1FINAL-8", "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
				if (Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break; // no nonce error so
				}
				// continue call with nonce
				++i;
			}
		}
	}

	protected void requestProtectedResource() {

		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + " Prepare Credential endpoint requests");

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		if (!isSecondClient()) {
			// these are optional; only add them for the first client
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");

			callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");

			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CDR-http-headers");
		}

		boolean mtlsRequired = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS || profileRequiresMtlsEverywhere;

		JsonObject mtls = null;
		if (!mtlsRequired) {
			mtls = env.getObject("mutual_tls_authentication");
			env.removeObject("mutual_tls_authentication");
		}
		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + " Call credential issuer nonce endpoint");
		// check for nonce endpoint
		JsonElement nonceEndpointEl = env.getElementFromObject("vci", "credential_issuer_metadata.nonce_endpoint");
		if (nonceEndpointEl != null) {

			callAndStopOnFailure(CallCredentialIssuerNonceEndpoint.class, "OID4VCI-1FINAL-7.1");

			eventLog.endBlock();

			eventLog.startBlock(currentClientString() + " Verify Credential Nonce Endpoint Response");
			call(exec().mapKey("endpoint_response", "nonce_endpoint_response"));
			callAndContinueOnFailure(new EnsureHttpStatusCode(200), ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");

			callAndContinueOnFailure(VCICheckCacheControlHeaderInResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
			callAndStopOnFailure(VCIValidateCredentialNonceResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		} else {
			eventLog.log(getName(), "Skipping nonce endpoint call - 'nonce_endpoint' not present in credential issuer metadata");
		}

		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + " Call Credential Endpoint");

		// use HTTP POST to call credentials endpoint
		env.putString("resource", "resourceMethod", "POST");
		env.putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		// TODO generate a key attestation here?
		callAndContinueOnFailure(VCIGenerateKeyAttestationJwt.class, ConditionResult.FAILURE, "HAIPA-D.1");

		callAndStopOnFailure(VCIGenerateProofJwt.class, "OID4VCI-1FINALA-F.1");

		callAndStopOnFailure(VCICreateCredentialRequest.class, "OID4VCI-1FINAL-8.2");

		if (isDpop()) {
			requestProtectedResourceUsingDpop();
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-8", "FAPI2-SP-ID2-5.3.3-2");
		}
		if (!mtlsRequired && mtls != null) {
			env.putObject("mutual_tls_authentication", mtls);
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + " Verify Credential Endpoint Response");
		// TODO: allow a deferred response with a transaction_id https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		callAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialResponse.class, ConditionResult.WARNING, "OID4VCI-1FINAL-8.3");
		callAndStopOnFailure(VCIExtractCredentialResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");

		callAndContinueOnFailure(ParseCredentialAsSdJwt.class, ConditionResult.FAILURE, "SDJWT-4");
		callAndContinueOnFailure(ValidateCredentialJWTIat.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-5.2.1");
		callAndContinueOnFailure(ValidateCredentialJWTVct.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-3.5.1");
		callAndContinueOnFailure(ValidateCredentialCnfJwkIsPublicKey.class, ConditionResult.FAILURE, "SDJWT-4.1.2");
		if (vciProfile == VCIProfile.HAIP) {
			callAndContinueOnFailure(VCIValidateCredentialValidityInfoIsPresent.class, ConditionResult.FAILURE, "HAIP-6.1-2.2");
			callAndContinueOnFailure(VCICheckExpClaimInCredential.class, ConditionResult.FAILURE, "HAIP-6.1-2.2");
			callAndContinueOnFailure(VCIValidateCredentialValidityByStatusListIfPresent.class, ConditionResult.FAILURE, "HAIP-6.1-2.4", "OTSL-6.2");
			callAndContinueOnFailure(VCIEnsureX5cHeaderPresentForSdJwtCredential.class, ConditionResult.FAILURE, "HAIP-6.1.1");
		}

		call(exec().unmapKey("endpoint_response"));
		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "RFC7231-7.1.1.2");

		skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO, CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		if (!isSecondClient()) {
			skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO, EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");
		}

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

	@VariantSetup(parameter = VCIClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		addParEndpointClientAuthentication = AddMTLSClientAuthenticationToPAREndpointRequest.class;
	}

	@VariantSetup(parameter = VCIClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest.class;

		if (getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS) {
			supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		}

		addParEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest.class;
	}

	@VariantSetup(parameter = VCIClientAuthType.class, value = "client_attestation")
	public void setupClientAttestation() {
		addTokenEndpointClientAuthentication = AddClientAttestationClientAuthToEndpointRequest.class;
		if (getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS) {
			supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		}
		addParEndpointClientAuthentication = AddClientAttestationClientAuthToEndpointRequest.class;
	}

	protected void generateClientAttestationKeys() {

		if (clientAuthType == VCIClientAuthType.CLIENT_ATTESTATION) {
			if (env.getString("config", "vci.client_attestation_issuer") == null) {
				throw new TestFailureException(getId(), "vci.client_attestation_issuer must be configured if client_attestation is configured as client authentication method.");
			}
		}

		callAndStopOnFailure(GenerateClientAttestationClientInstanceKey.class, ConditionResult.FAILURE, "OAuth2-ATCA07-1");
		callAndStopOnFailure(CreateClientAttestationJwt.class, ConditionResult.FAILURE, "OAuth2-ATCA07-1", "HAIP-4.3.1-2");

		// we generate a new CreateClientAttestationProofJwt via the AddClientAttestationClientAuthToEndpointRequest sequence
	}

	// @VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		preAuthorizationSteps = null;
		profileAuthorizationEndpointSetupSteps = null;
		profileIdTokenValidationSteps = null;
	}

	@VariantSetup(parameter = FAPI2SenderConstrainMethod.class, value = "dpop")
	public void setupCreateDpopForEndpointSteps() {
		createDpopForParEndpointSteps = () -> CreateDpopProofSteps.createParEndpointDpopSteps();
		createDpopForTokenEndpointSteps = () -> CreateDpopProofSteps.createTokenEndpointDpopSteps();
		createDpopForResourceEndpointSteps = () -> CreateDpopProofSteps.createResourceEndpointDpopSteps();
	}

	protected boolean scopeContains(String requiredScope) {
		String scope = env.getString("config", "client.scope");
		if (Strings.isNullOrEmpty(scope)) {
			throw new TestFailureException(getId(), "'scope' seems to be missing from client configuration");
		}
		List<String> scopes = Arrays.asList(scope.split(" "));
		return scopes.contains(requiredScope);
	}

	protected void performPARRedirectWithRequestUri() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}


	/**
	 * Call Par endpoint with retry for DPoP nonce error
	 *
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallParEndpoint)
	 */
	protected void callParEndpointAndStopOnFailure(String... requirements) {
		if (isDpop() && useDpopAuthCodeBinding) {
			final int MAX_RETRY = 2;
			int i = 0;
			while (i < MAX_RETRY) {
				createDpopForParEndpoint();
				callAndStopOnFailure(CallPAREndpointAllowingDpopNonceError.class, requirements);
				if (Strings.isNullOrEmpty(env.getString("par_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		} else {
			callAndStopOnFailure(CallPAREndpoint.class, requirements);
		}
	}


	protected void performParAuthorizationRequestFlow() {

		// we only need to (and only should) supply an MTLS authentication when using MTLS client auth;
		// there's no need to pass mtls auth when using private_key_jwt
		boolean mtlsRequired = clientAuthType == VCIClientAuthType.MTLS || profileRequiresMtlsEverywhere;

		JsonObject mtls = null;
		if (!mtlsRequired) {
			mtls = env.getObject("mutual_tls_authentication");
			env.removeObject("mutual_tls_authentication");
		}

		callParEndpointAndStopOnFailure("PAR-2.1");

		if (!mtlsRequired && mtls != null) {
			env.putObject("mutual_tls_authentication", mtls);
		}

		processParResponse();
	}

	protected void processParResponse() {
		callAndStopOnFailure(CheckPAREndpointResponse201WithNoError.class, "PAR-2.2", "PAR-2.3");

		callAndStopOnFailure(CheckForRequestUriValue.class, "PAR-2.2");

		callAndContinueOnFailure(CheckForPARResponseExpiresIn.class, ConditionResult.FAILURE, "PAR-2.2");

		callAndStopOnFailure(ExtractRequestUriFromPARResponse.class);

		callAndContinueOnFailure(EnsureMinimumRequestUriEntropy.class, ConditionResult.FAILURE, "PAR-2.2", "PAR-7.1", "JAR-10.2");

		performPARRedirectWithRequestUri();
	}
}
