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
import net.openid.conformance.condition.client.EnsureCredentialTrustAnchorConfigured;
import net.openid.conformance.condition.client.EnsureStatusListTrustAnchorConfigured;
import net.openid.conformance.condition.client.RegisterCredentialTrustAnchor;
import net.openid.conformance.condition.client.RegisterStatusListTrustAnchor;
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
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule;
import net.openid.conformance.fapi2spfinal.VCIProfileBehavior;
import net.openid.conformance.openid.federation.CallCredentialIssuerNonceEndpoint;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CreateVCICredentialRequestSteps;
import net.openid.conformance.sequence.client.GenerateVCIKeyAttestationAndProofSteps;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.sequence.client.ValidateVCINonceEndpointResponse;
import net.openid.conformance.testmodule.IterateEnvironmentArray;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.vci10issuer.condition.VCIAddCredentialResponseEncryptionToRequest;
import net.openid.conformance.vci10issuer.condition.VCICheckCredentialRequestEncryptionSupported;
import net.openid.conformance.vci10issuer.condition.VCICheckCredentialResponseEncryptionSupported;
import net.openid.conformance.vci10issuer.condition.VCIEncryptCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCIEnsureCredentialRequestEncryptionWhenResponseEncryptionOptional;
import net.openid.conformance.vci10issuer.condition.VCIEnsureCredentialRequestEncryptionWhenResponseEncryptionRequired;
import net.openid.conformance.vci10issuer.condition.VCICheckForDeferredCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCICreateDeferredCredentialRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateNotificationRequest;
import net.openid.conformance.vci10issuer.condition.VCICreateTokenEndpointRequestForPreAuthorizedCodeGrant;
import net.openid.conformance.vci10issuer.condition.VCIDecryptCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIEnsureCredentialResponseIsEncryptedJwe;
import net.openid.conformance.vci10issuer.condition.VCIEnsureCredentialResponseIsNotAnEncryptedJwe;
import net.openid.conformance.vci10issuer.condition.VCIEnsureIntervalPresentInDeferredResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractNotificationIdFromCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIExtractPreAuthorizedCodeAndTxCodeFromCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIExtractTxCodeFromRequest;
import net.openid.conformance.vci10issuer.condition.VCIFetchCredentialOfferFromCredentialOfferUri;
import net.openid.conformance.vci10issuer.condition.VCIGenerateClientJwksIfMissing;
import net.openid.conformance.vci10issuer.condition.VCIGenerateCredentialEncryptionJwks;
import net.openid.conformance.vci10issuer.condition.VCIGenerateRichAuthorizationRequestForCredential;
import net.openid.conformance.vci10issuer.condition.VCIResolveDeferredCredentialEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCIResolveNotificationEndpointToUse;
import net.openid.conformance.vci10issuer.condition.VCITryAddingIssuerStateToAuthorizationRequest;
import net.openid.conformance.vci10issuer.condition.VCITryToExtractIssuerStateFromCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIUseStaticTxCodeFromConfig;
import net.openid.conformance.vci10issuer.condition.CheckForUnexpectedParametersInCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialOfferRequestParams;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VCIValidateNoUnknownKeysInCredentialResponse;
import net.openid.conformance.vci10issuer.condition.VCIWaitForCredentialOffer;
import net.openid.conformance.vci10issuer.condition.VCIWaitForTxCode;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@VariantParameters({
	VCIGrantType.class,
	VCIAuthorizationCodeFlowVariant.class,
	VCI1FinalCredentialFormat.class,
	VCICredentialEncryption.class,
})
@ConfigurationFields({
	"vci.credential_issuer_url",
	"client.client_id",
	"client.jwks",
	"vci.credential_configuration_id",
	"vci.credential_proof_type_hint",
	"vci.key_attestation_jwks",
	"vci.authorization_server",
})
// VCI grant type configuration
@VariantConfigurationFields(parameter = VCIGrantType.class, value = "pre_authorization_code",
	configurationFields = {"vci.static_tx_code"})
// VCI flow variant hides
@VariantHidesConfigurationFields(parameter = VCIAuthorizationCodeFlowVariant.class, value = "wallet_initiated",
	configurationFields = {"vci.credential_offer_endpoint"})
@VariantNotApplicableWhen(
	parameter = VCIGrantType.class,
	values = {"pre_authorization_code"},
	whenParameter = FAPI2FinalOPProfile.class,
	hasValues = "vci_haip"
)
// Client attestation configuration field hides
@VariantHidesConfigurationFields(parameter = ClientAuthType.class, value = "client_attestation",
	configurationFields = {"client.jwks"})
@VariantHidesConfigurationFields(parameter = ClientAuthType.class, value = "private_key_jwt",
	configurationFields = {"vci.client_attestation_issuer", "vci.client_attestation_trust_anchor"})
@VariantHidesConfigurationFields(parameter = ClientAuthType.class, value = "mtls",
	configurationFields = {"vci.client_attestation_issuer", "vci.client_attestation_trust_anchor"})
public abstract class AbstractVCIIssuerTestModule extends AbstractFAPI2SPFinalServerTestModule {

	protected ClientAuthType clientAuthType;

	protected VCIGrantType vciGrantType;
	protected VCIAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant;

	protected VCI1FinalCredentialFormat vciCredentialFormat;

	protected VCICredentialEncryption vciCredentialEncryption;

	// --- Configuration overrides ---

	protected void initializeVciVariants() {
		clientAuthType = getVariant(ClientAuthType.class);
		vciGrantType = getVariant(VCIGrantType.class);
		vciAuthorizationCodeFlowVariant = getVariant(VCIAuthorizationCodeFlowVariant.class);
		vciCredentialFormat = getVariant(VCI1FinalCredentialFormat.class);
		vciCredentialEncryption = getVariant(VCICredentialEncryption.class);
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		initializeVciVariants();
		List<String> requiredProofTypes = getRequiredProofTypes();
		if (!requiredProofTypes.isEmpty()) {
			env.putString("vci_required_proof_types", String.join(",", requiredProofTypes));
		}
		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);
	}

	/**
	 * Ordered preference list of proof types this test needs ("jwt", "attestation"). The first entry
	 * that is advertised in the credential configuration's proof_types_supported wins. Returning an
	 * empty list (the default) lets the config hint and first-available fallback decide.
	 *
	 * Use this to make per-proof-type negative tests self-selecting when the issuer supports multiple
	 * proof types, so a single plan run covers both. The subclass's start() is still expected to fire
	 * fireTestSkipped if none of the preferred proof types is present.
	 */
	protected List<String> getRequiredProofTypes() {
		return List.of();
	}

	/**
	 * The profile behavior for VCI issuer tests is always either {@link VCIProfileBehavior} or
	 * {@link net.openid.conformance.fapi2spfinal.VCIHaipProfileBehavior}, set via the
	 * {@code vci} / {@code vci_haip} {@link net.openid.conformance.variant.VariantSetup} methods on
	 * the FAPI2SPFinal parent. Provide a typed accessor so VCI-specific delegation doesn't need to
	 * cast at every call site.
	 */
	protected VCIProfileBehavior vciProfileBehavior() {
		return (VCIProfileBehavior) profileBehavior;
	}

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
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			callAndStopOnFailure(VCIGenerateCredentialEncryptionJwks.class);
		}

		callAndStopOnFailure(RegisterCredentialTrustAnchor.class);
		callAndStopOnFailure(RegisterStatusListTrustAnchor.class);

		call(profileBehavior.configureClientExtra());
		call(profileBehavior.configureClientAttestation());

		validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			callAndStopOnFailure(VCICheckCredentialResponseEncryptionSupported.class, "OID4VCI-1FINAL-12.2.4");
			callAndStopOnFailure(VCICheckCredentialRequestEncryptionSupported.class, "OID4VCI-1FINAL-12.2.4");

			boolean responseDeclared = env.getElementFromObject("vci",
				"credential_issuer_metadata.credential_response_encryption") != null;
			boolean requestDeclared = env.getElementFromObject("vci",
				"credential_issuer_metadata.credential_request_encryption") != null;

			if (!responseDeclared && !requestDeclared) {
				fireTestSkipped("Credential encryption is not supported by the credential issuer"
					+ " - neither credential_response_encryption nor credential_request_encryption are"
					+ " present in the credential issuer metadata.");
				return;
			}

			// The encrypted variant of this test needs the wallet to be able to encrypt credential
			// requests, so in both §8.2 sub-cases (encryption_required=true and =false) a missing
			// credential_request_encryption means we cannot proceed — stop on either failure.
			callAndStopOnFailure(VCIEnsureCredentialRequestEncryptionWhenResponseEncryptionRequired.class, "OID4VCI-1FINAL-8.2", "OID4VCI-1FINAL-12.2.4");
			callAndStopOnFailure(VCIEnsureCredentialRequestEncryptionWhenResponseEncryptionOptional.class, "OID4VCI-1FINAL-8.2", "OID4VCI-1FINAL-12.2.4");
		}

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
		callAndContinueOnFailure(CheckForUnexpectedParametersInCredentialOffer.class, ConditionResult.WARNING, "OID4VCI-1FINAL-4.1");

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
	protected void callSenderConstrainedTokenEndpoint(String... requirements) {
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
			generateKeyAttestationAndProof();
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
					"OID4VCI-1FINAL-8", "FAPI2-SP-FINAL-5.3.4-2");
				if (Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		}
	}

	protected void createCredentialRequest() {
		call(makeCreateCredentialRequestSteps());
	}

	protected ConditionSequence makeCreateCredentialRequestSteps() {
		return new CreateVCICredentialRequestSteps(vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED);
	}

	/**
	 * Generate key attestation (if necessary) and proof for the credential request.
	 * Called when cryptographic binding is required.
	 */
	protected void generateKeyAttestationAndProof() {
		call(makeGenerateKeyAttestationAndProofSteps());
	}

	protected ConditionSequence makeGenerateKeyAttestationAndProofSteps() {
		String proofTypeKey = env.getString("vci_proof_type_key");
		return new GenerateVCIKeyAttestationAndProofSteps(proofTypeKey);
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
			generateKeyAttestationAndProof();
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

		call(new IterateEnvironmentArray("extracted_credentials", "list",
				() -> vciProfileBehavior().verifyCredential())
			.currentString("credential")
			.logBlockLabels(ctx -> ctx.getIterationCount() > 1
				? currentClientString() + "Verify credential " + ctx.getIteration() + " of " + ctx.getIterationCount()
				: currentClientString() + "Verify credential"));

		call(exec().unmapKey("endpoint_response"));
		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, ConditionResult.FAILURE, "RFC7231-7.1.1.2");

		skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO, CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		if (!isSecondClient()) {
			skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO, EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");
		}

		sendNotificationIfSupported();
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

		// Per OID4VCI 1.0 Final Section 9.1, the Deferred Credential Request takes the same
		// credential_response_encryption parameter as the initial Credential Request, and
		// Deferred Credential Request encryption MUST be used when credential_response_encryption
		// is included. Add the response encryption params and then encrypt the request body.
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			callAndStopOnFailure(VCIAddCredentialResponseEncryptionToRequest.class, "OID4VCI-1FINAL-9.1", "OID4VCI-1FINAL-8.2");

			JsonObject deferredRequest = env.getObject("vci_credential_request_object");
			env.putString("resource_request_entity", deferredRequest.toString());
		}

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		env.putString("resource_endpoint_request_headers", "Content-Type", "application/json");

		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			callAndStopOnFailure(VCIEncryptCredentialRequest.class, "OID4VCI-1FINAL-9.1", "OID4VCI-1FINAL-10");
		}

		if (isDpop()) {
			requestProtectedResourceUsingDpop();
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "OID4VCI-1FINAL-9", "FAPI2-SP-FINAL-5.3.4-2");
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

		callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(200, 202), ConditionResult.FAILURE, "OID4VCI-1FINAL-9.2");

		// Per OID4VCI 1.0 Final §9.2, a Deferred Credential Response is encrypted (as
		// application/jwt) when credential_response_encryption was requested, otherwise it is
		// application/json. §9.2 does not carve out a special rule for a still-pending 202 —
		// only the non-normative example shows application/json — so we deliberately treat a
		// 202 as JSON here to match the worked example, and warn rather than fail if a
		// transmitter returns application/jwt for a 202.
		int deferredStatusCode = env.getInteger("endpoint_response", "status");
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED && deferredStatusCode == 200) {
			callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-9.2", "OID4VCI-1FINAL-10");
		} else {
			callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.WARNING, "OID4VCI-1FINAL-9.2");
		}

		eventLog.endBlock();
	}

	// --- Nonce and proof hooks ---

	protected void afterNonceEndpointResponse() {
		call(exec().mapKey("endpoint_response", "nonce_endpoint_response"));
		call(new ValidateVCINonceEndpointResponse());
	}

	// --- Second client support ---

	@Override
	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
	}
}
