package net.openid.conformance.vci10issuer;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallProtectedResourceAllowingDpopNonceError;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCode;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs2xx;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.ParseCredentialAsSdJwt;
import net.openid.conformance.condition.client.ParseMdocCredentialFromVCIIssuance;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.ValidateCredentialIsUnpaddedBase64Url;
import net.openid.conformance.condition.client.ValidateCredentialJWTHeaderTyp;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateCredentialJWTIssIsHttpsUri;
import net.openid.conformance.condition.client.ValidateCredentialJWTVct;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.client.ValidateMdocIssuerSignedSignature;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule;
import net.openid.conformance.openid.federation.CallCredentialIssuerNonceEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.vci10issuer.condition.CheckCacheControlHeaderContainsNoStore;
import net.openid.conformance.vci10issuer.condition.VCIAddCredentialResponseEncryptionToRequest;
import net.openid.conformance.vci10issuer.condition.VCICheckExpClaimInCredential;
import net.openid.conformance.vci10issuer.condition.VCICheckForDeferredCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCICheckKeyAttestationJwksIfKeyAttestationIsRequired;
import net.openid.conformance.vci10issuer.condition.VCICreateCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateDeferredCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateNotificationRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateTokenEndpointRequestForPreAuthorizedCodeGrant;
import net.openid.conformance.vci10issuer.condition.VCIDecryptCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIDetermineCredentialConfigurationTransferMethod;
import net.openid.conformance.vci10issuer.condition.VCIEnsureCredentialResponseIsEncryptedJwe;
import net.openid.conformance.vci10issuer.condition.VCIEnsureCredentialResponseIsNotAnEncryptedJwe;
import net.openid.conformance.vci10issuer.condition.VCIEnsureIntervalPresentInDeferredResponse;
import net.openid.conformance.vci10issuer.condition.VCIEnsureResolvedCredentialConfigurationMatchesSelection;
import net.openid.conformance.vci10issuer.condition.VCIEnsureScopePresentInCredentialConfigurationForHaip;
import net.openid.conformance.vci10issuer.condition.VCIEnsureX5cHeaderPresentForSdJwtCredential;
import net.openid.conformance.vci10issuer.condition.VCIExtractCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractNotificationIdFromCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractPreAuthorizedCodeAndTxCodeFromCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIExtractTxCodeFromRequest;
import net.openid.conformance.vci10issuer.condition.VCIFetchCredentialOfferFromCredentialOfferUri;
import net.openid.conformance.vci10issuer.condition.VCIGenerateAttestationProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateClientJwksIfMissing;
import net.openid.conformance.vci10issuer.condition.VCIGenerateCredentialEncryptionJwks;
import net.openid.conformance.vci10issuer.condition.VCIGenerateJwtProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateKeyAttestationIfNecessary;
import net.openid.conformance.vci10issuer.condition.VCIGenerateRichAuthorizationRequestForCredential;
import net.openid.conformance.vci10issuer.condition.VCIResolveCredentialProofTypeToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveDeferredCredentialEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveNotificationEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveRequestedCredentialConfiguration;
import net.openid.conformance.vci10issuer.condition.VCITryAddingIssuerStateToAuthorizationRequest;
import net.openid.conformance.vci10issuer.condition.VCITryToExtractIssuerStateFromCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIUseStaticTxCodeFromConfig;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialNonceResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialOfferRequestParams;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialValidityInfoIsPresent;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIWaitForCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIWaitForTxCode;
import net.openid.conformance.vci10issuer.condition.statuslist.VCIValidateCredentialValidityByStatusListIfPresent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.servlet.ModelAndView;

@ConfigurationFields({
	"vci.credential_issuer_url",
	"client.client_id",
	"client.jwks",
	"vci.credential_configuration_id",
	"vci.credential_proof_type_hint",
	"vci.key_attestation_jwks",
	"vci.authorization_server",
})
public abstract class AbstractVCIIssuerTestModule extends AbstractFAPI2SPFinalServerTestModule {

	protected ClientAuthType clientAuthType;

	protected FAPI2FinalOPProfile fapi2Profile;

	protected VCIGrantType vciGrantType;
	protected VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant;

	protected VCI1FinalCredentialFormat vciCredentialFormat;

	protected VCICredentialEncryption vciCredentialEncryption;

	// --- Configuration overrides ---

	@Override
	protected void configureClient() {
		callAndStopOnFailure(GetStaticClientConfiguration.class);
		callAndStopOnFailure(VCIGenerateClientJwksIfMissing.class);

		exposeEnvString("client_id");

		boolean mtlsRequired = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS
			|| getVariant(ClientAuthType.class) == ClientAuthType.MTLS
			|| profileRequiresMtlsEverywhere;

		if (mtlsRequired) {
			callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
		}

		// Load credential encryption JWKS if encryption is enabled
		if (getVariant(VCICredentialEncryption.class) == VCICredentialEncryption.ENCRYPTED) {
			callAndStopOnFailure(VCIGenerateCredentialEncryptionJwks.class);
		}

		if (getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.VCI_HAIP) {
			setupHaipClients();
		}

		validateClientConfiguration();
	}

	protected void setupHaipClients() {
		env.putString("client", "dpop_signing_alg", "ES256");
		env.putString("client2", "dpop_signing_alg", "ES256");
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		// Initialize VCI-specific variant fields
		clientAuthType = getVariant(ClientAuthType.class);
		fapi2Profile = getVariant(FAPI2FinalOPProfile.class);
		vciGrantType = getVariant(VCIGrantType.class);
		vciAuthorizationCodeFlowVariant = getVariant(VCIAuthorizationCodeFlowVariant.class);
		vciCredentialFormat = getVariant(VCI1FinalCredentialFormat.class);
		vciCredentialEncryption = getVariant(VCICredentialEncryption.class);

		// Check if encryption is supported by the issuer
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			JsonElement algValuesEl = env.getElementFromObject("vci",
				"credential_issuer_metadata.credential_response_encryption.alg_values_supported");
			JsonElement encValuesEl = env.getElementFromObject("vci",
				"credential_issuer_metadata.credential_response_encryption.enc_values_supported");

			if (algValuesEl == null || encValuesEl == null || !algValuesEl.isJsonArray() || !encValuesEl.isJsonArray()) {
				fireTestSkipped("Encryption is not supported by credential issuer"
					+ " - credential_response_encryption.alg_values_supported"
					+ " and/or credential_response_encryption.enc_values_supported"
					+ " missing or invalid in issuer metadata.");
				return;
			}
		}

		// Resolve credential configuration
		determineCredentialConfigurationTransferMethod();

		// Store credential_resource_url for later use (before notification/deferred endpoints may overwrite it)
		env.putString("credential_resource_url", env.getString("resource", "resourceUrl"));

		// Client attestation keys are generated by VCIProfileBehavior.configureClientAttestation()
		// which runs before onConfigure(). Validate configuration and call the hook so subclasses
		// can modify the attestation (e.g. negative tests that invalidate the signature).
		if (clientAuthType == ClientAuthType.CLIENT_ATTESTATION) {
			if (env.getString("config", "vci.client_attestation_issuer") == null) {
				throw new TestFailureException(getId(),
					"'Client Attestation Issuer' must be configured in the 'VCI' section "
						+ "in the test configuration when client_attestation is the client authentication method.");
			}
			afterClientAttestationGenerated();
		}
	}

	protected void determineCredentialConfigurationTransferMethod() {

		resolveCredentialConfigurationId();

		// HAIP requires scope to be present for every credential configuration
		if (fapi2Profile == FAPI2FinalOPProfile.VCI_HAIP) {
			callAndContinueOnFailure(VCIEnsureScopePresentInCredentialConfigurationForHaip.class, ConditionResult.FAILURE, "HAIP-4.1", "HAIP-4.3");
		}

		callAndStopOnFailure(VCIDetermineCredentialConfigurationTransferMethod.class, ConditionResult.FAILURE);
		callAndStopOnFailure(VCIResolveCredentialProofTypeToUse.class, ConditionResult.FAILURE);

		// Only check key attestation if cryptographic binding is required
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			callAndStopOnFailure(VCICheckKeyAttestationJwksIfKeyAttestationIsRequired.class, ConditionResult.FAILURE);
		}
	}

	protected void resolveCredentialConfigurationId() {
		String vciCredentialConfigurationId = env.getString("config", "vci.credential_configuration_id");
		if (vciCredentialConfigurationId == null || vciCredentialConfigurationId.isBlank()) {
			throw new TestFailureException(getId(), "credential_configuration_id cannot be null or empty!");
		}
		exposeEnvString("credential_configuration_id", "config", "vci.credential_configuration_id");
		env.putString("vci_credential_configuration_id", vciCredentialConfigurationId);
		callAndStopOnFailure(VCIResolveRequestedCredentialConfiguration.class, ConditionResult.FAILURE);
		callAndStopOnFailure(new VCIEnsureResolvedCredentialConfigurationMatchesSelection(vciCredentialFormat));
	}

	protected void generateClientAttestationKeys() {
		call(profileBehavior.configureClientAttestation());
		afterClientAttestationGenerated();
	}

	/**
	 * Hook called after the client attestation JWT is generated.
	 * Override this method in subclasses to modify the client attestation.
	 */
	protected void afterClientAttestationGenerated() {
		// Default implementation does nothing
	}

	// --- Test lifecycle overrides ---

	@Override
	public void start() {

		setStatus(Status.RUNNING);
		switch (vciAuthorizationCodeFlowVariant) {

			case WALLET_INITIATED -> {
				switch (vciGrantType) {
					case AUTHORIZATION_CODE -> performAuthorizationFlow();
					case PRE_AUTHORIZATION_CODE ->
						throw new UnsupportedOperationException(
							"Pre-authorization code is not supported for wallet initiated flow");
				}
			}

			case ISSUER_INITIATED -> {
				waitForCredentialOffer();
			}
		}
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res,
							HttpSession session, JsonObject requestParts) {

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

	// --- Credential offer and tx_code handling ---

	protected void waitForCredentialOffer() {
		expose("credential_offer_endpoint", env.getString("base_url") + "/credential_offer");
		callAndStopOnFailure(VCIWaitForCredentialOffer.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-4.1");
		setStatus(Status.WAITING);
	}

	protected Object handleCredentialOffer(HttpServletRequest req, HttpServletResponse res,
										HttpSession session, JsonObject requestParts) {

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

	protected Object handleTxCode() {

		setStatus(Status.RUNNING);
		callAndStopOnFailure(VCIExtractTxCodeFromRequest.class,
			ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5");
		performPreAuthorizationCodeFlow();

		return new ModelAndView("resultCaptured",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	protected void waitForTxCode() {
		expose("tx_code_endpoint", env.getString("base_url") + "/tx_code?code=your_tx_code");
		callAndStopOnFailure(VCIWaitForTxCode.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5");

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
		}
	}

	protected void performPreAuthorizationCodeFlow() {

		callAndStopOnFailure(VCIExtractPreAuthorizedCodeAndTxCodeFromCredentialOffer.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5", "OID4VCI-1FINAL-4.1.1");

		performPostAuthorizationFlow();
	}

	// --- Authorization request overrides ---

	private static class CreateAuthorizationRequestSteps extends AbstractConditionSequence {

		private boolean isSecondClient;
		private boolean isOpenId;
		private boolean usePkce;
		private Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;
		private final VCIGrantType vciGrantType;
		private final VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant;

		public CreateAuthorizationRequestSteps(boolean isSecondClient, boolean isOpenId, boolean usePkce,
											Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps,
											VCIGrantType vciGrantType,
											VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant) {
			this.isSecondClient = isSecondClient;
			this.isOpenId = isOpenId;
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

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		ConditionSequence seq = new CreateAuthorizationRequestSteps(isSecondClient(), isOpenId, usePkce, profileAuthorizationEndpointSetupSteps, vciGrantType, vciAuthorizationCodeFlowVariant);
		if (isRarRequest) {
			seq.then(condition(VCIGenerateRichAuthorizationRequestForCredential.class).onFail(ConditionResult.FAILURE).requirements("OID4VCI-1FINAL-5.1.1"));
			seq.then(condition(RARSupport.AddRARToAuthorizationEndpointRequest.class));
		}
		return seq;
	}

	// --- Token endpoint overrides ---

	@Override
	protected void performPostAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Call token endpoint");

		// call the token endpoint and complete the flow
		switch (vciGrantType) {
			case AUTHORIZATION_CODE -> createAuthorizationCodeRequest();
			case PRE_AUTHORIZATION_CODE -> createPreAuthorizationCodeRequest();
		}

		exchangeAuthorizationCode();
		requestProtectedResource();
		onPostAuthorizationFlowComplete();
	}

	protected void createPreAuthorizationCodeRequest() {
		callAndStopOnFailure(VCICreateTokenEndpointRequestForPreAuthorizedCodeGrant.class);
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		if (env.getObject("token_endpoint_request_headers") == null) {
			env.putObject("token_endpoint_request_headers", new JsonObject());
		}

		addPkceCodeVerifier();
	}

	/**
	 * VCI adds client authentication inside the DPoP retry loop (unlike FAPI2 base which adds it before).
	 */
	@Override
	protected void callSenderConstrainedTokenEndpointAndStopOnFailure(String... requirements) {
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
			callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class, requirements);
		}
	}

	@Override
	protected void exchangeAuthorizationCode() {
		callSenderConstrainedTokenEndpointAndCheckForHttp200();

		eventLog.startBlock(currentClientString() + "Verify token endpoint response");
		processTokenEndpointResponse();
		eventLog.endBlock();
	}

	// --- PAR overrides ---

	@Override
	protected void performPARRedirectWithRequestUri() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		callAndStopOnFailure(
			BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}

	// --- VCI-specific credential endpoint logic ---

	@Override
	protected void requestProtectedResource() {

		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + " Prepare Credential endpoint requests");

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		if (!isSecondClient()) {
			// These FAPI headers are optional for VCI but included for interoperability
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);

			callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class);

			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);
		}

		boolean mtlsRequired = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS || profileRequiresMtlsEverywhere;

		JsonObject mtls = null;
		if (!mtlsRequired) {
			mtls = env.getObject("mutual_tls_authentication");
			env.removeObject("mutual_tls_authentication");
		}
		eventLog.endBlock();

		// Check if the credential configuration requires cryptographic binding
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			eventLog.startBlock(currentClientString() + " Call credential issuer nonce endpoint");
			// check for nonce endpoint
			JsonElement nonceEndpointEl = env.getElementFromObject("vci", "credential_issuer_metadata.nonce_endpoint");
			if (nonceEndpointEl != null) {

				callAndStopOnFailure(CallCredentialIssuerNonceEndpoint.class, "OID4VCI-1FINAL-7.1");

				eventLog.endBlock();

				eventLog.startBlock(currentClientString() + " Verify Credential Nonce Endpoint Response");
				afterNonceEndpointResponse();
			} else {
				eventLog.log(getName(), "Skipping nonce endpoint call - 'nonce_endpoint' not present in credential issuer metadata");
			}

			eventLog.endBlock();
		} else {
			eventLog.log(getName(), "Skipping nonce endpoint call - credential configuration does not require cryptographic binding");
		}

		eventLog.startBlock(currentClientString() + " Call Credential Endpoint");

		// use HTTP POST to call credentials endpoint
		env.putString("resource", "resourceMethod", "POST");
		env.putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			// determine if requested credential requires key attestation
			callAndContinueOnFailure(VCIGenerateKeyAttestationIfNecessary.class, ConditionResult.FAILURE, "HAIPA-D.1", "OID4VCI-1FINALA-D.1");

			afterKeyAttestationGeneration();

			String proofTypeKey = env.getString("vci_proof_type_key");
			if ("jwt".equals(proofTypeKey)) {
				callAndStopOnFailure(VCIGenerateJwtProof.class, "OID4VCI-1FINALA-F.1");
			} else if ("attestation".equals(proofTypeKey)) {
				callAndStopOnFailure(VCIGenerateAttestationProof.class, "OID4VCI-1FINALA-F.3");
			}

			afterProofGeneration();
		} else {
			eventLog.log(getName(), "Skipping proof generation - credential configuration does not require cryptographic binding");
		}

		createCredentialRequest();

		if (isDpop()) {
			requestProtectedResourceUsingDpop();
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-8", "FAPI2-SP-FINAL-5.3.4-2");
		}
		if (!mtlsRequired && mtls != null) {
			env.putObject("mutual_tls_authentication", mtls);
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + " Verify Credential Endpoint Response");
		verifyCredentialIssuerCredentialResponse();

		eventLog.endBlock();
	}

	@Override
	protected void requestProtectedResourceUsingDpop() {
		if (isDpop() && (createDpopForResourceEndpointSteps != null)) {
			final int MAX_RETRY = 2;
			int i = 0;
			while (i < MAX_RETRY) {
				call(sequence(createDpopForResourceEndpointSteps));
				callAndStopOnFailure(CallProtectedResourceAllowingDpopNonceError.class,
					"OID4VCI-1FINAL-8", "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
				if (Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		}
	}

	protected void createCredentialRequest() {
		callAndStopOnFailure(VCICreateCredentialRequest.class, "OID4VCI-1FINAL-8.2");

		// Add encryption parameters if encryption is enabled
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			callAndStopOnFailure(VCIAddCredentialResponseEncryptionToRequest.class, "OID4VCI-1FINAL-8.2");
			afterCredentialResponseEncryptionAdded();
		}

		JsonObject credentialRequestObject = env.getObject("vci_credential_request_object");
		String requestBodyString = serializeCredentialRequestObject(credentialRequestObject);
		env.putString("resource_request_entity", requestBodyString);
	}

	/**
	 * Hook called after credential_response_encryption is added to the credential request.
	 * Override this method in subclasses to modify the encryption parameters.
	 */
	protected void afterCredentialResponseEncryptionAdded() {
		// Default implementation does nothing
	}

	protected String serializeCredentialRequestObject(JsonObject credentialRequestObject) {
		return credentialRequestObject.toString();
	}

	/**
	 * Refresh the credential request by re-fetching a nonce, regenerating proof/key attestation,
	 * and recreating the credential request body. Use this instead of updateResourceRequest() when
	 * calling the credential endpoint again after a successful response, as the wallet consumes
	 * the nonce on first use.
	 */
	protected void refreshCredentialRequest() {
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			JsonElement nonceEndpointEl = env.getElementFromObject("vci",
				"credential_issuer_metadata.nonce_endpoint");
			if (nonceEndpointEl != null) {
				callAndStopOnFailure(CallCredentialIssuerNonceEndpoint.class, "OID4VCI-1FINAL-7.1");
				afterNonceEndpointResponse();
			}
		}

		// Ensure the resource URL points to the credential endpoint
		String credentialResourceUrl = env.getString("credential_resource_url");
		env.putString("resource", "resourceUrl", credentialResourceUrl);
		env.putString("protected_resource_url", credentialResourceUrl);
		env.putString("resource", "resourceMethod", "POST");
		env.putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			callAndContinueOnFailure(VCIGenerateKeyAttestationIfNecessary.class,
				ConditionResult.FAILURE, "HAIPA-D.1", "OID4VCI-1FINALA-D.1");
			afterKeyAttestationGeneration();

			String proofTypeKey = env.getString("vci_proof_type_key");
			if ("jwt".equals(proofTypeKey)) {
				callAndStopOnFailure(VCIGenerateJwtProof.class, "OID4VCI-1FINALA-F.1");
			} else if ("attestation".equals(proofTypeKey)) {
				callAndStopOnFailure(VCIGenerateAttestationProof.class, "OID4VCI-1FINALA-F.3");
			}
			afterProofGeneration();
		}

		createCredentialRequest();
	}

	// --- Credential response verification ---

	/**
	 * Verifies a successful credential endpoint response. Asserts HTTP 200, handles decryption
	 * if needed, then delegates to verifyEffectiveCredentialResponse() for the actual validation.
	 * Subclasses that expect error responses should override this method to call
	 * verifyCredentialIssuerCredentialErrorResponse() instead.
	 */
	protected void verifyCredentialIssuerCredentialResponse() {
		// Accept both 200 (immediate) and 202 (deferred) — the distinction is handled
		// in verifyEffectiveCredentialResponse() after checking for transaction_id
		callAndStopOnFailure(new EnsureHttpStatusCodeIsAnyOf(200, 202), "OID4VCI-1FINAL-8.3");

		// Decrypt the response if encryption was requested
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
			callAndStopOnFailure(VCIEnsureCredentialResponseIsEncryptedJwe.class, "OID4VCI-1FINAL-8.3.1.2");
			callAndStopOnFailure(VCIDecryptCredentialResponse.class, "OID4VCI-1FINAL-10");
		} else {
			callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		}

		verifyEffectiveCredentialResponse();
	}

	protected void verifyCredentialIssuerCredentialErrorResponse() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3.1");
		callAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialErrorResponse.class, Condition.ConditionResult.WARNING, "OID4VCI-1FINAL-8.3.1");
		// Note that Credential Error Responses are never encrypted, even if a valid Credential Response would be.
		callAndContinueOnFailure(VCIEnsureCredentialResponseIsNotAnEncryptedJwe.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3.1.2");
	}

	/**
	 * Verifies the effective credential response - i.e., the response after potential decryption.
	 */
	protected void verifyEffectiveCredentialResponse() {
		callAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialResponse.class, ConditionResult.WARNING, "OID4VCI-1FINAL-8.3");

		// Check if the response is deferred (contains transaction_id instead of credentials)
		callAndStopOnFailure(VCICheckForDeferredCredentialResponse.class, "OID4VCI-1FINAL-9");

		String isDeferredStr = env.getString("deferred_credential_response");
		boolean isDeferred = "true".equals(isDeferredStr);

		if (isDeferred) {
			// Deferred response - need to call the deferred credential endpoint
			callAndContinueOnFailure(new EnsureHttpStatusCode(202), ConditionResult.WARNING, "OID4VCI-1FINAL-9");

			callAndContinueOnFailure(VCIEnsureIntervalPresentInDeferredResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-9.3");

			call(exec().unmapKey("endpoint_response"));

			// Poll the deferred credential endpoint
			callDeferredCredentialEndpoint();

			// Map the deferred response for validation
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

			int statusCode = env.getInteger("endpoint_response", "status");

			// Decrypt the deferred response if encryption was requested and OK
			if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED && statusCode == 200) {
				callAndStopOnFailure(VCIEnsureCredentialResponseIsEncryptedJwe.class, "OID4VCI-1FINAL-8.3.1.2");
				callAndStopOnFailure(VCIDecryptCredentialResponse.class, "OID4VCI-1FINAL-10");
			}
		} else {
			// Immediate response - credential is in the response
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		}

		// Extract and validate all credentials (same for both paths)
		callAndStopOnFailure(VCIExtractCredentialResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");

		// Iterate over all extracted credentials and validate each one
		JsonArray extractedCredentials = env.getObject("extracted_credentials").getAsJsonArray("list");
		for (int i = 0; i < extractedCredentials.size(); i++) {
			String credential = OIDFJSON.getString(extractedCredentials.get(i));
			env.putString("credential", credential);

			if (extractedCredentials.size() > 1) {
				eventLog.startBlock(currentClientString() + "Verify credential " + (i + 1) + " of " + extractedCredentials.size());
			} else {
				eventLog.startBlock(currentClientString() + "Verify credential");
			}

			verifyCredential();

			eventLog.endBlock();
		}

		call(exec().unmapKey("endpoint_response"));
		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, ConditionResult.FAILURE, "RFC7231-7.1.1.2");

		skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO, CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		if (!isSecondClient()) {
			skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO, EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");
		}

		sendNotificationIfSupported();
	}

	/**
	 * Verifies a single credential from the credential response.
	 */
	protected void verifyCredential() {
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");

		if (vciCredentialFormat == VCI1FinalCredentialFormat.MDOC) {
			callAndContinueOnFailure(ValidateCredentialIsUnpaddedBase64Url.class,
				ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2.4");
			callAndContinueOnFailure(ParseMdocCredentialFromVCIIssuance.class,
				ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
			callAndContinueOnFailure(ValidateMdocIssuerSignedSignature.class,
				ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
		} else if (vciCredentialFormat == VCI1FinalCredentialFormat.SD_JWT_VC) {
			callAndContinueOnFailure(ParseCredentialAsSdJwt.class,
				ConditionResult.FAILURE, "SDJWT-4");
			callAndContinueOnFailure(ValidateCredentialJWTIssIsHttpsUri.class,
				ConditionResult.FAILURE, "SDJWTVC-3.2.2.2");
			callAndContinueOnFailure(ValidateCredentialJWTIat.class,
				ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-5.2");
			callAndContinueOnFailure(ValidateCredentialJWTVct.class,
				ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-3.5");
			callAndContinueOnFailure(ValidateCredentialJWTHeaderTyp.class,
				ConditionResult.FAILURE, "SDJWTVC-3.2.1");
			if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
				callAndContinueOnFailure(ValidateCredentialCnfJwkIsPublicKey.class,
					ConditionResult.FAILURE, "SDJWT-4.1.2");
			}
			if (fapi2Profile == FAPI2FinalOPProfile.VCI_HAIP) {
				callAndContinueOnFailure(VCIValidateCredentialValidityInfoIsPresent.class,
					ConditionResult.WARNING, "HAIP-6.1-2.2");
				callAndContinueOnFailure(VCICheckExpClaimInCredential.class,
					ConditionResult.FAILURE, "HAIP-6.1-2.2");
				callAndContinueOnFailure(VCIValidateCredentialValidityByStatusListIfPresent.class,
					ConditionResult.FAILURE, "HAIP-6.1-2.4", "OTSL-6.2");
				callAndContinueOnFailure(VCIEnsureX5cHeaderPresentForSdJwtCredential.class,
					ConditionResult.FAILURE, "HAIP-6.1.1");
			}
		}
	}

	// --- Notification endpoint ---

	/**
	 * Sends a credential_accepted notification to the issuer's notification endpoint if supported.
	 */
	protected void sendNotificationIfSupported() {

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(VCIExtractNotificationIdFromCredentialResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		call(exec().unmapKey("endpoint_response"));

		String notificationId = env.getString("notification_id");
		if (notificationId == null) {
			eventLog.log(getName(), "No notification_id in credential response, skipping attempt to send a notification");
			eventLog.endBlock();
			return;
		}

		eventLog.startBlock(currentClientString() + "Send Notification to Issuer");

		callAndStopOnFailure(VCIResolveNotificationEndpointToUse.class, "OID4VCI-1FINAL-12.2.4");
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);

		env.putString("resource", "resourceMethod", "POST");

		createNotificationRequest();

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		env.putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		if (isDpop()) {
			requestProtectedResourceUsingDpop();
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-11", "FAPI2-SP-FINAL-5.3.4-2");
		}
		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + " Validate Notification Response from Issuer");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		validateNotificationEndpointResponse();
		call(exec().unmapKey("endpoint_response"));
		eventLog.endBlock();

		// Restore the credential endpoint URL
		String credentialResourceUrl = env.getString("credential_resource_url");
		env.putString("resource", "resourceUrl", credentialResourceUrl);
		env.putString("protected_resource_url", credentialResourceUrl);
	}

	protected void validateNotificationEndpointResponse() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs2xx.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-11.2");
	}

	protected void createNotificationRequest() {
		callAndStopOnFailure(VCICreateNotificationRequest.class, "OID4VCI-1FINAL-11.1");
	}

	// --- Deferred credential endpoint ---

	/**
	 * Calls the deferred credential endpoint to retrieve the credential.
	 */
	protected void callDeferredCredentialEndpoint() {
		eventLog.startBlock(currentClientString() + "Call Deferred Credential Endpoint");

		callAndStopOnFailure(VCIResolveDeferredCredentialEndpointToUse.class, "OID4VCI-1FINAL-12.2.4");
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);

		env.putString("resource", "resourceMethod", "POST");

		callAndStopOnFailure(VCICreateDeferredCredentialRequest.class, "OID4VCI-1FINAL-9.1");

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		env.putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		if (isDpop()) {
			requestProtectedResourceUsingDpop();
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-9", "FAPI2-SP-FINAL-5.3.4-2");
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

		callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(200, 202), ConditionResult.FAILURE, "OID4VCI-1FINAL-9.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.WARNING, "OID4VCI-1FINAL-9.2");

		eventLog.endBlock();
	}

	// --- Nonce and proof hooks ---

	protected void afterNonceEndpointResponse() {
		call(exec().mapKey("endpoint_response", "nonce_endpoint_response"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.WARNING, "OID4VCI-1FINAL-7.2");

		callAndContinueOnFailure(CheckCacheControlHeaderContainsNoStore.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndStopOnFailure(VCIValidateCredentialNonceResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
	}

	/**
	 * Hook called after key attestation generation but before proof generation.
	 */
	protected void afterKeyAttestationGeneration() {
		// Default implementation does nothing
	}

	/**
	 * Hook called after proof generation but before creating the credential request.
	 */
	protected void afterProofGeneration() {
		// Default implementation does nothing
	}

	// --- Second client support ---

	@Override
	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

		if (env.getString("config", "client.scope") != null && env.getString("client", "scope") == null) {
			env.putString("client", "scope", env.getString("config", "client.scope"));
		}
	}
}
