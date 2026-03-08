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
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallProtectedResourceAllowingDpopNonceError;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckPAREndpointResponse201WithNoError;
import net.openid.conformance.condition.client.CheckForPARResponseExpiresIn;
import net.openid.conformance.condition.client.CheckForRequestUriValue;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCode;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs2xx;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMinimumRequestUriEntropy;
import net.openid.conformance.condition.client.ExtractRequestUriFromPARResponse;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule;
import net.openid.conformance.openid.federation.CallCredentialIssuerNonceEndpoint;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.vci10issuer.condition.CheckCacheControlHeaderContainsNoStore;
import net.openid.conformance.vci10issuer.condition.VCIAddCredentialResponseEncryptionToRequest;
import net.openid.conformance.vci10issuer.condition.VCICheckExpClaimInCredential;
import net.openid.conformance.vci10issuer.condition.VCICheckForDeferredCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCICreateCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateDeferredCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateNotificationRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateTokenEndpointRequestForPreAuthorizedCodeGrant;
import net.openid.conformance.vci10issuer.condition.VCIDecryptCredentialResponse;
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
import net.openid.conformance.vci10issuer.condition.VCIGenerateJwtProof;
import net.openid.conformance.vci10issuer.condition.VCIGenerateKeyAttestationIfNecessary;
import net.openid.conformance.vci10issuer.condition.VCIGenerateRichAuthorizationRequestForCredential;
import net.openid.conformance.vci10issuer.condition.VCIResolveCredentialEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveDeferredCredentialEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveNotificationEndpointToUse;
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
import net.openid.conformance.condition.client.ParseCredentialAsSdJwt;
import net.openid.conformance.condition.client.ParseMdocCredentialFromVCIIssuance;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.ValidateCredentialIsUnpaddedBase64Url;
import net.openid.conformance.condition.client.ValidateCredentialJWTHeaderTyp;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateCredentialJWTIssIsHttpsUri;
import net.openid.conformance.condition.client.ValidateCredentialJWTVct;
import net.openid.conformance.condition.client.ValidateMdocIssuerSignedSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.servlet.ModelAndView;

@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {
	"plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "fapi_client_credentials_grant"
})
public abstract class AbstractVCIIssuerTestModule extends AbstractFAPI2SPFinalServerTestModule {

	protected VCIGrantType vciGrantType;
	protected VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant;
	protected VCI1FinalCredentialFormat vciCredentialFormat;
	protected VCICredentialEncryption vciCredentialEncryption;

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		vciGrantType = getVariant(VCIGrantType.class);
		vciAuthorizationCodeFlowVariant = getVariant(VCIAuthorizationCodeFlowVariant.class);
		vciCredentialFormat = getVariant(VCI1FinalCredentialFormat.class);
		vciCredentialEncryption = getVariant(VCICredentialEncryption.class);

		// HAIP-specific validations
		VCIProfile vciProfile = getVariant(VCIProfile.class);
		if (vciProfile == VCIProfile.HAIP && isRarRequest) {
			throw new TestFailureException(getId(), "The usage of authorization request type RAR is not supported with HAIP.");
		}
		if (vciProfile == VCIProfile.HAIP && vciGrantType == VCIGrantType.PRE_AUTHORIZATION_CODE) {
			throw new TestFailureException(getId(), "The usage of grant type Pre-Authorized Code Flow is not supported with HAIP.");
		}

		// Resolve and validate credential configuration matching the selected format
		resolveCredentialConfigurationId();
	}

	protected void resolveCredentialConfigurationId() {
		String vciCredentialConfigurationId = env.getString("config", "vci.credential_configuration_id");
		if (vciCredentialConfigurationId == null || vciCredentialConfigurationId.isBlank()) {
			throw new TestFailureException(getId(), "credential_configuration_id cannot be null or empty!");
		}
		exposeEnvString("credential_configuration_id", "config", "vci.credential_configuration_id");
		env.putString("vci_credential_configuration_id", vciCredentialConfigurationId);
		callAndStopOnFailure(new VCIEnsureResolvedCredentialConfigurationMatchesSelection(vciCredentialFormat));
	}

	// HAIP requires scope to be present for every credential configuration
	protected void ensureHaipScopeIfRequired() {
		VCIProfile vciProfile = getVariant(VCIProfile.class);
		if (vciProfile == VCIProfile.HAIP) {
			callAndContinueOnFailure(VCIEnsureScopePresentInCredentialConfigurationForHaip.class, ConditionResult.FAILURE, "HAIP-4.1", "HAIP-4.3");
		}
	}

	// --- start() and HTTP handlers ---

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
			case ISSUER_INITIATED -> waitForCredentialOffer();
		}
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

	protected void waitForCredentialOffer() {
		expose("credential_offer_endpoint", env.getString("base_url") + "/credential_offer");
		callAndStopOnFailure(VCIWaitForCredentialOffer.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-4.1");
		setStatus(Status.WAITING);
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
			ImmutableMap.of("returnUrl", "/log-detail.html?log=" + getId()));
	}

	protected Object handleTxCode() {
		setStatus(Status.RUNNING);
		callAndStopOnFailure(VCIExtractTxCodeFromRequest.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5");
		performPreAuthorizationCodeFlow();
		return new ModelAndView("resultCaptured",
			ImmutableMap.of("returnUrl", "/log-detail.html?log=" + getId()));
	}

	protected void waitForTxCode() {
		expose("tx_code_endpoint", env.getString("base_url") + "/tx_code?code=your_tx_code");
		callAndStopOnFailure(VCIWaitForTxCode.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5");
		setStatus(Status.WAITING);
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

	// --- Authorization request customization ---

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		ConditionSequence seq = new VCICreateAuthorizationRequestSteps(isSecondClient(), usePkce, profileAuthorizationEndpointSetupSteps, vciGrantType, vciAuthorizationCodeFlowVariant);
		if (isRarRequest) {
			seq.then(condition(VCIGenerateRichAuthorizationRequestForCredential.class).onFail(ConditionResult.FAILURE).requirements("OID4VCI-1FINAL-5.1.1"));
			seq.then(condition(RARSupport.AddRARToAuthorizationEndpointRequest.class));
		}
		return seq;
	}

	private static class VCICreateAuthorizationRequestSteps extends AbstractConditionSequence {

		private boolean isSecondClient;
		private boolean usePkce;
		private Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;
		private final VCIGrantType vciGrantType;
		private final VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant;

		public VCICreateAuthorizationRequestSteps(boolean isSecondClient, boolean usePkce, Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps, VCIGrantType vciGrantType, VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant) {
			this.isSecondClient = isSecondClient;
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

	// --- Token endpoint: VCI defers client auth to retry loop ---

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		if (env.getObject("token_endpoint_request_headers") == null) {
			env.putObject("token_endpoint_request_headers", new JsonObject());
		}

		addPkceCodeVerifier();
		// Note: client auth is NOT added here — it's added in callSenderConstrainedTokenEndpointAndStopOnFailure
		// on each retry, so attestation PoP freshness is maintained
	}

	@Override
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

	// --- Post-authorization flow: handles both auth code and pre-auth code ---

	@Override
	protected void performPostAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Call token endpoint");

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

	// --- PAR: VCI uses different redirect builder ---

	@Override
	protected void performPARRedirectWithRequestUri() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}

	// --- PAR response: VCI version is slightly simpler ---

	@Override
	protected void processParResponse() {
		callAndStopOnFailure(CheckPAREndpointResponse201WithNoError.class, "PAR-2.2", "PAR-2.3");
		callAndStopOnFailure(CheckForRequestUriValue.class, "PAR-2.2");
		callAndContinueOnFailure(CheckForPARResponseExpiresIn.class, ConditionResult.FAILURE, "PAR-2.2");
		callAndStopOnFailure(ExtractRequestUriFromPARResponse.class);
		callAndContinueOnFailure(EnsureMinimumRequestUriEntropy.class, ConditionResult.FAILURE, "PAR-2.2", "PAR-7.1", "JAR-10.2");
		performPARRedirectWithRequestUri();
	}

	// --- Credential endpoint (protected resource) ---

	@Override
	protected void requestProtectedResource() {
		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + " Prepare Credential endpoint requests");

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		if (!isSecondClient()) {
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
			callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CDR-http-headers");
		}

		boolean mtlsRequired = isMTLS() || profileRequiresMtlsEverywhere;

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

	protected void createCredentialRequest() {
		callAndStopOnFailure(VCICreateCredentialRequest.class, "OID4VCI-1FINAL-8.2");

		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			callAndStopOnFailure(VCIAddCredentialResponseEncryptionToRequest.class, "OID4VCI-1FINAL-8.2");
			afterCredentialResponseEncryptionAdded();
		}

		JsonObject credentialRequestObject = env.getObject("vci_credential_request_object");
		String requestBodyString = serializeCredentialRequestObject(credentialRequestObject);
		env.putString("resource_request_entity", requestBodyString);
	}

	protected void afterCredentialResponseEncryptionAdded() {
		// Default implementation does nothing
	}

	protected String serializeCredentialRequestObject(JsonObject credentialRequestObject) {
		return credentialRequestObject.toString();
	}

	/**
	 * Refresh the credential request by re-fetching a nonce, regenerating proof/key attestation,
	 * and recreating the credential request body.
	 */
	protected void refreshCredentialRequest() {
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			JsonElement nonceEndpointEl = env.getElementFromObject("vci", "credential_issuer_metadata.nonce_endpoint");
			if (nonceEndpointEl != null) {
				callAndStopOnFailure(CallCredentialIssuerNonceEndpoint.class, "OID4VCI-1FINAL-7.1");
				afterNonceEndpointResponse();
			}
		}

		String credentialResourceUrl = env.getString("credential_resource_url");
		env.putString("resource", "resourceUrl", credentialResourceUrl);
		env.putString("protected_resource_url", credentialResourceUrl);
		env.putString("resource", "resourceMethod", "POST");
		env.putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
			callAndContinueOnFailure(VCIGenerateKeyAttestationIfNecessary.class, ConditionResult.FAILURE, "HAIPA-D.1", "OID4VCI-1FINALA-D.1");
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

	protected void verifyCredentialIssuerCredentialResponse() {
		callAndStopOnFailure(EnsureHttpStatusCodeIs200.class, "OID4VCI-1FINAL-8.3");

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
		callAndContinueOnFailure(VCIEnsureCredentialResponseIsNotAnEncryptedJwe.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3.1.2");
	}

	protected void verifyEffectiveCredentialResponse() {
		callAndContinueOnFailure(VCIValidateNoUnknownKeysInCredentialResponse.class, ConditionResult.WARNING, "OID4VCI-1FINAL-8.3");

		callAndStopOnFailure(VCICheckForDeferredCredentialResponse.class, "OID4VCI-1FINAL-9");

		String isDeferredStr = env.getString("deferred_credential_response");
		boolean isDeferred = "true".equals(isDeferredStr);

		if (isDeferred) {
			callAndContinueOnFailure(new EnsureHttpStatusCode(202), ConditionResult.WARNING, "OID4VCI-1FINAL-9");
			callAndContinueOnFailure(VCIEnsureIntervalPresentInDeferredResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-9.3");
			call(exec().unmapKey("endpoint_response"));
			callDeferredCredentialEndpoint();
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

			int statusCode = env.getInteger("endpoint_response", "status");
			if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED && statusCode == 200) {
				callAndStopOnFailure(VCIEnsureCredentialResponseIsEncryptedJwe.class, "OID4VCI-1FINAL-8.3.1.2");
				callAndStopOnFailure(VCIDecryptCredentialResponse.class, "OID4VCI-1FINAL-10");
			}
		} else {
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");
		}

		callAndStopOnFailure(VCIExtractCredentialResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.3");

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

	protected void verifyCredential() {
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		VCIProfile vciProfile = getVariant(VCIProfile.class);

		if (vciCredentialFormat == VCI1FinalCredentialFormat.MDOC) {
			callAndContinueOnFailure(ValidateCredentialIsUnpaddedBase64Url.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2.4");
			callAndContinueOnFailure(ParseMdocCredentialFromVCIIssuance.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
			callAndContinueOnFailure(ValidateMdocIssuerSignedSignature.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
		} else if (vciCredentialFormat == VCI1FinalCredentialFormat.SD_JWT_VC) {
			callAndContinueOnFailure(ParseCredentialAsSdJwt.class, ConditionResult.FAILURE, "SDJWT-4");
			callAndContinueOnFailure(ValidateCredentialJWTIssIsHttpsUri.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2");
			callAndContinueOnFailure(ValidateCredentialJWTIat.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-5.2");
			callAndContinueOnFailure(ValidateCredentialJWTVct.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-3.5");
			callAndContinueOnFailure(ValidateCredentialJWTHeaderTyp.class, ConditionResult.FAILURE, "SDJWTVC-3.2.1");
			if (requiresCryptographicBinding != null && requiresCryptographicBinding) {
				callAndContinueOnFailure(ValidateCredentialCnfJwkIsPublicKey.class, ConditionResult.FAILURE, "SDJWT-4.1.2");
			}
			if (vciProfile == VCIProfile.HAIP) {
				callAndContinueOnFailure(VCIValidateCredentialValidityInfoIsPresent.class, ConditionResult.WARNING, "HAIP-6.1-2.2");
				callAndContinueOnFailure(VCICheckExpClaimInCredential.class, ConditionResult.FAILURE, "HAIP-6.1-2.2");
				callAndContinueOnFailure(VCIValidateCredentialValidityByStatusListIfPresent.class, ConditionResult.FAILURE, "HAIP-6.1-2.4", "OTSL-6.2");
				callAndContinueOnFailure(VCIEnsureX5cHeaderPresentForSdJwtCredential.class, ConditionResult.FAILURE, "HAIP-6.1.1");
			}
		}
	}

	// --- Deferred credential endpoint ---

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

	// --- Notification endpoint ---

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

	// --- Nonce/proof hooks ---

	protected void afterNonceEndpointResponse() {
		call(exec().mapKey("endpoint_response", "nonce_endpoint_response"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.WARNING, "OID4VCI-1FINAL-7.2");
		callAndContinueOnFailure(CheckCacheControlHeaderContainsNoStore.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndStopOnFailure(VCIValidateCredentialNonceResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
	}

	protected void afterKeyAttestationGeneration() {
		// Default implementation does nothing
	}

	protected void afterProofGeneration() {
		// Default implementation does nothing
	}

	public void afterClientAttestationGenerated() {
		// Default implementation does nothing
	}

	// --- DPoP resource endpoint (adds VCI requirements) ---

	@Override
	protected void requestProtectedResourceUsingDpop() {
		if (isDpop() && (createDpopForResourceEndpointSteps != null)) {
			final int MAX_RETRY = 2;
			int i = 0;
			while (i < MAX_RETRY) {
				call(sequence(createDpopForResourceEndpointSteps));
				callAndStopOnFailure(CallProtectedResourceAllowingDpopNonceError.class, "OID4VCI-1FINAL-8", "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
				if (Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		}
	}

	// --- Resource endpoint URL setup ---

	@Override
	protected void setupResourceEndpoint() {
		callAndStopOnFailure(VCIResolveCredentialEndpointToUse.class);
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		env.putString("credential_resource_url", env.getString("resource", "resourceUrl"));
	}
}
