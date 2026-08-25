package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreateMdocCredentialForVCI;
import net.openid.conformance.condition.as.CreateSdJwtCredential;
import net.openid.conformance.condition.as.GenerateCredentialNonce;
import net.openid.conformance.condition.as.GenerateCredentialNonceResponse;
import net.openid.conformance.condition.client.BuildVCIDCAPIRequest;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.CreateResourceEndpointDpopErrorResponse;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIWalletAuthorizationCodeFlowVariant;
import net.openid.conformance.vci10wallet.VCICredentialConfigurations;
import net.openid.conformance.vci10wallet.VCICredentialIssuerMetadataBuilder;
import net.openid.conformance.vci10wallet.condition.CheckForUnexpectedParametersInCredentialRequest;
import net.openid.conformance.vci10wallet.condition.VCIAddNotificationIdToCredentialEndpointResponse;
import net.openid.conformance.vci10wallet.condition.VCICheckForUnknownFieldsInNotificationRequest;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialEndpointResponse;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOffer;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOfferRedirectUrl;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOfferUri;
import net.openid.conformance.vci10wallet.condition.VCIEnsureBearerAccessTokenNotInParams;
import net.openid.conformance.vci10wallet.condition.VCIEnsureCredentialSigningCertificateIsNotSelfSigned;
import net.openid.conformance.vci10wallet.condition.VCIExtractCredentialRequestProof;
import net.openid.conformance.vci10wallet.condition.VCIGenerateIssuerState;
import net.openid.conformance.vci10wallet.condition.VCIInjectCredentialConfigurationIdHint;
import net.openid.conformance.vci10wallet.condition.VCIPreparePreAuthorizationCode;
import net.openid.conformance.vci10wallet.condition.VCIResolveRequestedCredentialConfigurationFromRequest;
import net.openid.conformance.vci10wallet.condition.VCISetCredentialFormatFlag;
import net.openid.conformance.vci10wallet.condition.VCISetProofTypeFlag;
import net.openid.conformance.vci10wallet.condition.VCIValidateAttestedKeysInKeyAttestationFromJwtProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestAttestationProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestDiVpProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestJwtProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestStructure;
import net.openid.conformance.vci10wallet.condition.VCIValidateNotificationRequest;
import net.openid.conformance.vci10wallet.condition.VCIVerifyIssuerStateInAuthorizationRequest;
import net.openid.conformance.vci10wallet.condition.ValidateKeyAttestationX5cCertificateChain;
import net.openid.conformance.condition.as.clientattestation.AddClientAttestationSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterClientAttestationTrustAnchor;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterKeyAttestationTrustAnchor;
import net.openid.conformance.vci10wallet.condition.statuslist.VCIGenerateJwtStatusListToken;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Profile behavior for VCI (Verifiable Credentials Issuance) client tests.
 *
 * <p>The conformance suite acts as the issuer that the wallet under test interacts with.
 * For the FAPI2SP client tests pulled into the VCI wallet plan, this behavior layers
 * VCI-specific endpoints (credential issuer metadata, nonce, credential, notification)
 * on top of the standard OAuth flow that {@link AbstractFAPI2SPFinalClientTest}
 * implements, so paired issuer tests can run a complete HAIP credential issuance flow
 * against a FAPI2SP client test.
 *
 * <p>Scope of this behavior:
 * <ul>
 *   <li>Immediate credential issuance (deferred is not implemented here)</li>
 *   <li>Plain (non-encrypted) credential responses</li>
 *   <li>JWT and attestation proof types (di_vp passes through to the existing condition)</li>
 *   <li>Both mso_mdoc and SD-JWT VC formats</li>
 *   <li>HAIP-style status list claim included in SD-JWT credentials</li>
 *   <li>Wallet-initiated and issuer-initiated authorization code flow. For the
 *       issuer-initiated flow ({@code vci_authorization_code_flow_variant=issuer_initiated})
 *       we generate an {@code issuer_state}, create a credential offer (by value or by
 *       reference, per {@code vci_credential_offer_variant}), hand it to the front-end as
 *       URL / QR code in {@link #onStart()}, serve the offer from
 *       {@code credential_offer/{id}} for the by-reference case, and verify the
 *       {@code issuer_state} the wallet echoes back in its authorization request.</li>
 * </ul>
 *
 * <p>The VCI wallet variants ({@code VCIWalletAuthorizationCodeFlowVariant},
 * {@code VCICredentialOfferParameterVariant}, {@code VCI1FinalCredentialFormat}, ...) are not
 * declared by the FAPI2SP client modules themselves: {@code VCIWalletTestPlanHaip} marks them
 * as plan-level context and {@code VariantService} injects the user's selection into the
 * module's variant map, so they are read via {@code getVariantOrDefault} here and fall back
 * to the variant's default when the module runs outside that plan.
 *
 * <p>End-to-end credential issuance with encryption / deferred issuance lives on
 * {@code AbstractVCIWalletTest}'s own modules ({@code oid4vci-1_0-wallet-test-credential-issuance}).
 */
public class VCIClientProfileBehavior extends FAPI2ClientProfileBehavior {

	private static final String CREDENTIAL_PATH = "credential";
	private static final String CREDENTIAL_OFFER_PATH = "credential_offer";
	private static final String NONCE_PATH = "nonce";
	private static final String NOTIFICATION_PATH = "notification";
	private static final String STATUSLISTS_PATH = "statuslists";

	@Override
	public ConditionSequence validateAuthorizationRequestScope() {
		// VCI requested scopes correspond to credential_configuration_id values from the
		// credential issuer metadata (e.g. org.iso.18013.5.1.mDL), not the test's
		// client.scope. Skip the strict scope-equality check; AbstractVCIWalletTest
		// does the same in its checkRequestedScopes() override.
		return null;
	}

	@Override
	public ConditionSequence additionalServerConfiguration() {
		Environment env = module.getEnv();
		boolean clientAttestation = module.clientAuthType == ClientAuthType.CLIENT_ATTESTATION;

		if (clientAttestation) {
			// Validate the wallet test config has the client_attestation fields populated.
			// The fields are declared via @VariantConfigurationFields on AbstractFAPI2SPFinalClientTest
			// so the schedule-test UI prompts for them; this catches the case where they're left blank.
			// Each check accepts either the new client_attestation.* key or the legacy vci.* key
			// during the transition window.
			if (env.getString("config", "client_attestation.issuer") == null
				&& env.getString("config", "vci.client_attestation_issuer") == null) {
				throw new TestFailureException(module.getId(),
					"'Client Attestation Issuer' field is missing from the 'Client Attestation' section in the test configuration");
			}
			if (env.getString("config", "client_attestation.trust_anchor") == null
				&& env.getString("config", "vci.client_attestation_trust_anchor") == null) {
				throw new TestFailureException(module.getId(),
					"'Client Attestation Trust Anchor' field is missing from the 'Client Attestation' section in the test configuration");
			}
		}

		// Pre-populate credential_issuer_metadata + credential_configurations_supported
		// so the VCI conditions invoked from the credential / nonce endpoints (and the
		// issuer-initiated credential offer) find them. This is independent of the client
		// authentication method.
		JsonObject metadata;
		try {
			metadata = VCICredentialIssuerMetadataBuilder.buildCredentialIssuerMetadata(env,
				new VCICredentialIssuerMetadataBuilder.Config(
					CREDENTIAL_PATH,
					NONCE_PATH,
					/* deferredCredentialPath */ null,
					NOTIFICATION_PATH,
					/* useMtlsForResources */ module.isMTLSConstrain(),
					/* notificationsEnabled */ true,
					/* deferredEnabled */ false,
					/* encryptionEnabled */ false,
					/* batchSize */ null));
		} catch (IllegalStateException e) {
			throw new TestFailureException(module.getId(), e.getMessage());
		}
		env.putObject("credential_issuer_metadata", metadata);
		VCICredentialIssuerMetadataBuilder.configureSupportedCredentialConfigurations(env, metadata,
			VCICredentialConfigurations.getDefault(module.getId()));

		ConditionSequence issuerInitiatedSetup = issuerInitiatedSetupSteps();

		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				if (clientAttestation) {
					callAndStopOnFailure(AddClientAttestationSigningAlgValuesSupportedToServerConfiguration.class, "OAuth2-ATCA07-10.1");
					callAndStopOnFailure(VCIRegisterClientAttestationTrustAnchor.class);
				}
				// signing JWK required so we can issue real mdoc / SD-JWT credentials
				callAndStopOnFailure(VCIEnsureCredentialSigningCertificateIsNotSelfSigned.class, "HAIP-6.1.1");
				callAndStopOnFailure(VCIRegisterKeyAttestationTrustAnchor.class);
				if (issuerInitiatedSetup != null) {
					call(issuerInitiatedSetup);
				}
			}
		};
	}

	// --- Issuer-initiated authorization code flow (credential offer) ---

	/**
	 * The authorization code flow variant selected for the wallet plan this module runs in.
	 * Falls back to {@code wallet_initiated} (the variant's default) when the module runs
	 * outside the VCI wallet plan and so has no plan-level VCI context at all.
	 */
	private VCIWalletAuthorizationCodeFlowVariant authorizationCodeFlowVariant() {
		return module.getVariantOrDefault(VCIWalletAuthorizationCodeFlowVariant.class,
			VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED);
	}

	/**
	 * Whether the issuer (us) has to make the first move by presenting a credential offer
	 * to the wallet, rather than the wallet starting the flow on its own.
	 */
	public boolean isIssuerInitiated() {
		return authorizationCodeFlowVariant() != VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED;
	}

	/**
	 * Configure-time setup for the issuer-initiated flow, or {@code null} for
	 * wallet-initiated: generate the {@code issuer_state} that goes into the credential offer
	 * (and that the wallet must echo back in its authorization request, see
	 * {@link #additionalAuthorizationRequestChecks()}) and resolve the
	 * {@code credential_configuration_id} to offer ({@code vci.credential_configuration_id}
	 * from the test configuration, else the default for the selected credential format).
	 * The wallet modules do the equivalent in {@code AbstractVCIWalletTest.configure}.
	 */
	public ConditionSequence issuerInitiatedSetupSteps() {
		if (!isIssuerInitiated()) {
			return null;
		}
		VCIGrantType grantType = module.getVariantOrDefault(VCIGrantType.class, VCIGrantType.AUTHORIZATION_CODE);
		if (grantType != VCIGrantType.AUTHORIZATION_CODE) {
			// The FAPI2SP token endpoint only implements the authorization_code grant; the
			// pre-authorized code grant is exercised by AbstractVCIWalletTest's own modules.
			throw new TestFailureException(module.getId(),
				"The FAPI2 security profile client tests only support the authorization_code grant type; "
					+ "'" + grantType + "' is not supported in combination with the issuer-initiated flow here.");
		}
		VCI1FinalCredentialFormat credentialFormat = module.getVariantOrDefault(VCI1FinalCredentialFormat.class, null);
		String fallbackCredentialConfigurationId = defaultCredentialConfigurationId(credentialFormat);
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(VCIGenerateIssuerState.class, "OID4VCI-1FINAL-5.1.3-2.1");
				callAndStopOnFailure(new VCIInjectCredentialConfigurationIdHint(fallbackCredentialConfigurationId));
			}
		};
	}

	/**
	 * The {@code credential_configuration_id} offered / expected when the test configuration
	 * doesn't pin one via {@code vci.credential_configuration_id}. Shared with
	 * {@code AbstractVCIWalletTest.getDefaultCredentialConfigurationId()}.
	 */
	public static String defaultCredentialConfigurationId(VCI1FinalCredentialFormat credentialFormat) {
		if (credentialFormat == VCI1FinalCredentialFormat.MDOC) {
			return "eu.europa.ec.eudi.pid.mdoc.1";
		}
		return "eu.europa.ec.eudi.pid.1";
	}

	/**
	 * Credential offer creation for the issuer-initiated flow — the single implementation
	 * shared by the wallet modules ({@code AbstractVCIWalletTest.prepareCredentialOffer}) and
	 * the FAPI2SP client tests ({@link #onStart()}). Static + explicit arguments because the
	 * wallet overwrites {@code profileBehavior} with {@code PlainFAPIClientProfileBehavior} in
	 * its {@code configure} and so can't reach an instance of this class.
	 *
	 * <p>Steps: for the pre-authorized code grant mint the pre-authorized code + tx_code
	 * (OID4VCI 1.0 Final 3.5); create the offer itself (4.1); for {@code by_reference} a
	 * {@code credential_offer_uri} pointing at our {@code credential_offer/{id}} endpoint
	 * (4.1.3); and finally the redirect URL built from the configured
	 * {@code vci.credential_offer_endpoint} (the wallet's custom URL scheme / universal link)
	 * carrying either {@code credential_offer} or {@code credential_offer_uri}.
	 */
	public static ConditionSequence credentialOfferSteps(VCIGrantType grantType,
			VCICredentialOfferParameterVariant offerVariant) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				if (grantType == VCIGrantType.PRE_AUTHORIZATION_CODE) {
					callAndStopOnFailure(VCIPreparePreAuthorizationCode.class, "OID4VCI-1FINAL-3.5", "OID4VCI-1FINAL-4.1");
				}
				callAndStopOnFailure(new VCICreateCredentialOffer(grantType), "OID4VCI-1FINAL-4.1");
				if (offerVariant == VCICredentialOfferParameterVariant.BY_REFERENCE) {
					callAndStopOnFailure(VCICreateCredentialOfferUri.class, "OID4VCI-1FINAL-4.1.3");
				}
				callAndStopOnFailure(new VCICreateCredentialOfferRedirectUrl(offerVariant), "OID4VCI-1FINAL-4.1");
			}
		};
	}

	/**
	 * Create the credential offer ({@link #credentialOfferSteps}) and hand it to the wallet
	 * via the front-end: as a URL / QR code for {@code issuer_initiated}, or as a Digital
	 * Credentials API request for {@code issuer_initiated_dc_api}. Shared by the wallet
	 * modules and the FAPI2SP client tests (see {@link #credentialOfferSteps} for why this
	 * is static). Does not manage test status — the caller does.
	 */
	public static void prepareCredentialOffer(AbstractFAPI2SPFinalClientTest module, VCIGrantType grantType,
			VCIWalletAuthorizationCodeFlowVariant flowVariant, VCICredentialOfferParameterVariant offerVariant) {
		module.doCall(credentialOfferSteps(grantType, offerVariant));

		BrowserControl browser = module.getBrowser();
		browser.setShowQrCodes(true);

		if (flowVariant == VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED_DC_API) {
			module.doCallAndStopOnFailure(BuildVCIDCAPIRequest.class);
			JsonObject request = module.getEnv().getObject("browser_api_request");
			browser.requestCredential(request, ""); // FIXME for now, no submitUrl === it's a VCI request, not VP
		} else {
			String credentialOfferRedirectUrl = module.getEnv().getString("vci", "credential_offer_redirect_url");
			browser.goToUrl(credentialOfferRedirectUrl, null, "GET", 10);
		}
	}

	/**
	 * The credential offer steps for this module's plan-level variant selection; see
	 * {@link #credentialOfferSteps(VCIGrantType, VCICredentialOfferParameterVariant)}.
	 */
	public ConditionSequence credentialOfferSteps() {
		return credentialOfferSteps(
			module.getVariantOrDefault(VCIGrantType.class, VCIGrantType.AUTHORIZATION_CODE),
			module.getVariantOrDefault(VCICredentialOfferParameterVariant.class, VCICredentialOfferParameterVariant.BY_VALUE));
	}

	/**
	 * Issuer-initiated flow: present the credential offer to the wallet as soon as the test
	 * starts. Without this the FAPI2SP client tests in the VCI wallet plan just sat in
	 * WAITING with nothing for the wallet to scan (GitLab #1901).
	 */
	@Override
	public void onStart() {
		if (!isIssuerInitiated()) {
			return;
		}
		prepareCredentialOffer(module,
			module.getVariantOrDefault(VCIGrantType.class, VCIGrantType.AUTHORIZATION_CODE),
			authorizationCodeFlowVariant(),
			module.getVariantOrDefault(VCICredentialOfferParameterVariant.class, VCICredentialOfferParameterVariant.BY_VALUE));
	}

	/**
	 * Issuer-initiated flow: the wallet MUST echo the {@code issuer_state} from the credential
	 * offer in its authorization request (OID4VCI 1.0 Final 5.1.3). The FAPI2SP client tests
	 * always use PAR, so the parameters are read from {@code par_endpoint_http_request_params}
	 * just like {@code AbstractVCIWalletTest.authorizationEndpoint} does.
	 */
	@Override
	public ConditionSequence additionalAuthorizationRequestChecks() {
		if (!isIssuerInitiated()) {
			return null;
		}
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(VCIVerifyIssuerStateInAuthorizationRequest.class, "OID4VCI-1FINAL-5.1.3");
			}
		};
	}

	@Override
	public void exposeProfileEndpoints() {
		super.exposeProfileEndpoints();
		String credentialIssuer = baseUrlWithTrailingSlash();
		module.getEnv().putString("credential_issuer", credentialIssuer);
		module.exposeEnvStringForBehavior("credential_issuer");
	}

	@Override
	public Object handleProfileSpecificWellKnown(String path) {
		if (path.startsWith("/.well-known/openid-credential-issuer")) {
			JsonObject metadata = module.getEnv().getObject("credential_issuer_metadata");
			return ResponseEntity.status(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON)
				.body(metadata);
		}
		return null;
	}

	@Override
	public boolean claimsHttpPath(String path) {
		return CREDENTIAL_PATH.equals(path)
			|| NONCE_PATH.equals(path)
			|| NOTIFICATION_PATH.equals(path)
			|| isCredentialOfferPath(path)
			|| isStatusListsPath(path);
	}

	@Override
	public PathDispatch getProfileSpecificPathDispatch(String requestId, String path) {
		if (NONCE_PATH.equals(path)) {
			return buildNonceDispatch();
		}
		if (NOTIFICATION_PATH.equals(path)) {
			return buildNotificationDispatch();
		}
		if (isCredentialOfferPath(path)) {
			return buildCredentialOfferDispatch(path, null);
		}
		if (isStatusListsPath(path)) {
			// statuslists is served imperatively below — the response shape branches on
			// path (aggregation vs single list) and the JWT-token generation runs
			// conditionally on env state, neither of which fits the PathDispatch model.
			return null;
		}
		// CREDENTIAL_PATH
		return buildCredentialDispatch();
	}

	@Override
	public Object handleProfileSpecificPath(String requestId, String path) {
		if (isStatusListsPath(path)) {
			return serveStatusListsRequest(module, requestId, path);
		}
		return super.handleProfileSpecificPath(requestId, path);
	}

	public static boolean isStatusListsPath(String path) {
		return path.equals(STATUSLISTS_PATH) || path.startsWith(STATUSLISTS_PATH + "/");
	}

	/** {@code credential_offer/{id}} — the by-reference credential offer endpoint. */
	private static boolean isCredentialOfferPath(String path) {
		return path.startsWith(CREDENTIAL_OFFER_PATH + "/");
	}

	/**
	 * OID4VCI 1.0 Final 4.1.3 credential offer endpoint (by reference): the wallet
	 * dereferences the {@code credential_offer_uri} it was handed when the offer was
	 * presented. Single implementation shared by the FAPI2SP client tests
	 * ({@link #getProfileSpecificPathDispatch}, no additional checks) and the wallet
	 * modules ({@code AbstractVCIWalletTest.credentialOfferEndpoint}, which passes its
	 * mTLS certificate checks). Block and incoming_request bookkeeping belong to the
	 * driver ({@code AbstractFAPI2SPFinalClientTest.runPathDispatch}).
	 *
	 * <p>{@code additionalRequestChecks} are extra checks to run before the shared ones;
	 * may be {@code null}.
	 */
	public static PathDispatch buildCredentialOfferDispatch(String path, ConditionSequence additionalRequestChecks) {
		ConditionSequence sequence = new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				if (additionalRequestChecks != null) {
					call(additionalRequestChecks);
				}
			}
		};
		return new PathDispatch("Credential offer endpoint", sequence,
			m -> buildCredentialOfferResponse(m.getEnv(), path));
	}

	/**
	 * Response for a {@code credential_offer/{id}} request: the stored offer (JSON,
	 * {@code Cache-Control: no-cache}) when the id in the path matches the one minted by
	 * {@link VCICreateCredentialOfferUri}, 404 otherwise.
	 */
	public static ResponseEntity<Object> buildCredentialOfferResponse(Environment env, String path) {
		String credentialOfferId = path.substring(path.lastIndexOf('/') + 1);
		String expectedCredentialOfferId = env.getString("vci", "credential_offer_id");
		if (expectedCredentialOfferId != null && expectedCredentialOfferId.equals(credentialOfferId)) {
			JsonElement credentialOffer = env.getElementFromObject("vci", "credential_offer");
			return ResponseEntity.status(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.CACHE_CONTROL, "no-cache")
				.body(credentialOffer);
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}

	/**
	 * Status list endpoint per draft-ietf-oauth-status-list-15. Two shapes:
	 * <ul>
	 *   <li>{@code GET statuslists} (aggregation, §9.3) — JSON listing the individual status
	 *       list URLs the issuer publishes.</li>
	 *   <li>{@code GET statuslists/{id}} (individual list, §8.1) — signed status list JWT
	 *       (mediatype {@code application/statuslist+jwt}); 404 when no list with that id.</li>
	 * </ul>
	 *
	 * <p>Static + module-arg so the wallet (which overwrites {@code profileBehavior} to
	 * {@code PlainFAPIClientProfileBehavior} in its {@code configure}) can route directly to
	 * this method without going through the (non-VCI) profile-behavior dispatcher.
	 *
	 * <p>Does not manage test status — the caller is responsible (the FAPI2SP outer's
	 * {@code handleClientRequestForPath} wraps profile dispatch in a {@code RUNNING/WAITING}
	 * try/finally; the wallet wraps the call site itself).
	 */
	public static Object serveStatusListsRequest(AbstractFAPI2SPFinalClientTest module,
			String requestId, String path) {
		module.doCall(module.doExec().startBlock("Status list endpoint")
			.mapKey("status_list_endpoint_request", requestId));

		Environment env = module.getEnv();
		ResponseEntity<Object> response;
		if (path.equals(STATUSLISTS_PATH) || path.equals(STATUSLISTS_PATH + "/")) {
			JsonObject aggregatedStatusList = new JsonObject();
			JsonArray statusLists = new JsonArray();
			// we only support one list for now
			statusLists.add(getStatusListUrl(env, "1"));
			aggregatedStatusList.add("statuslists", statusLists);
			response = ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(aggregatedStatusList);
		} else {
			String statusListId = path.substring(path.lastIndexOf("/") + 1);
			JsonElement statusListEl = env.getElementFromObject("vci",
				"status_lists.status_list_" + statusListId);
			if (statusListEl == null) {
				response = ResponseEntity.notFound().build();
			} else {
				env.putString("current_status_list_id", statusListId);
				module.doCallAndContinueOnFailure(VCIGenerateJwtStatusListToken.class,
					ConditionResult.INFO, "OTSL-5.1");
				String currentStatusListJwt = env.getString("current_status_list_jwt");
				// TODO add cors headers
				// TODO handle time query parameter, see https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-15#section-8.4
				response = ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_TYPE, "application/statuslist+jwt")
					.body(currentStatusListJwt);
			}
		}

		module.doCall(module.doExec().unmapKey("status_list_endpoint_request").endBlock());
		return response;
	}

	static String getStatusListUrl(Environment env, String statusListId) {
		return env.getString("server", "issuer") + STATUSLISTS_PATH + "/" + statusListId;
	}

	/**
	 * Additional SD-JWT claims to inject into the issued credential. Default is none
	 * ({@code null}). Subclasses for profiles that require specific SD-JWT claims (e.g.
	 * the HAIP status_list reference, see {@link VCIHaipClientProfileBehavior}) override
	 * this to return a populated map. Public so the wallet's imperative credential-issuance
	 * path can read it without duplicating the claim-construction logic.
	 */
	public Map<String, Object> additionalSdJwtClaims() {
		return null;
	}

	/**
	 * OID4VCI 1.0 Final 7.2 nonce endpoint — generates a fresh c_nonce that the wallet
	 * binds into the credential request proof JWT. Single-sequence dispatch: condition
	 * sequence does the validation + generation; response builder reads the prepared
	 * response from env. Mirrors AbstractVCIWalletTest.nonceEndpoint.
	 */
	private PathDispatch buildNonceDispatch() {
		ConditionSequence sequence = new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.1.1");
				callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.1");
				callAndStopOnFailure(GenerateCredentialNonce.class, "OID4VCI-1FINAL-7");
				callAndStopOnFailure(GenerateCredentialNonceResponse.class, "OID4VCI-1FINAL-7.2");
			}
		};
		return new PathDispatch("Nonce endpoint", sequence, m -> {
			JsonObject body = m.getEnv().getObject("credential_nonce_response");
			JsonObject headers = m.getEnv().getObject("credential_nonce_response_headers");
			return ResponseEntity.status(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON)
				.headers(headersFromJson(headers))
				.body(body);
		});
	}

	/**
	 * OID4VCI 1.0 Final 8 credential endpoint — full validation chain in a single
	 * ConditionSequence. Self-gating via {@code skipIfElementPresent("vci",
	 * "credential_error_response")} on every step after the first validation failure
	 * preserves the short-circuit semantics; proof-type and credential-format branching
	 * are expressed via flag conditions ({@link VCISetProofTypeFlag},
	 * {@link VCISetCredentialFormatFlag}) plus {@code skipIfElementMissing} on the per-type
	 * flag keys. The response builder reads {@code vci.credential_error_response} at the
	 * end to decide between a 400 error response and the happy-path 200.
	 *
	 * <p><strong>Order note:</strong> sender-constrain validation now runs <em>before</em>
	 * the bearer-token-in-URL check (it was second in the prior imperative flow). Both
	 * are first-line request-shape checks that must pass; with sender-constrain conditions
	 * using {@code callAndStopOnFailure} semantics (test halts on failure), running them
	 * inside the same sequence requires this order. The only user-visible diff is when a
	 * request has both a malformed DPoP / mtls token AND the access token in URL params
	 * — original returned 400 (bearer-token error); the new path halts the test on the
	 * sender-constrain failure. Both indicate a broken client.
	 */
	private PathDispatch buildCredentialDispatch() {
		final Map<String, Object> additionalClaims = additionalSdJwtClaims();
		ConditionSequence sequence = new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// Resource-endpoint request validation: sender-constrain proof, access-token
				// validation (DPoP jkt + proof ath / mTLS), and FAPI resource headers.
				// Halts on hard failures inside the embedded sequence.
				call(module.checkResourceEndpointRequestSequence(false));

				// DPoP-nonce-error short-circuit: when sender-constrain populated
				// resource_endpoint_dpop_nonce_error (stale or missing DPoP nonce), build the
				// prepared 401 DPoP error response. Subsequent c_nonce-consuming validation
				// is gated below via skipIfStringPresent so the wallet's c_nonce isn't burned
				// on this attempt — the response lambda short-circuits to this response.
				call(condition(CreateResourceEndpointDpopErrorResponse.class)
					.skipIfStringMissing("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE)
					.dontStopOnFailure());

				// Validation phase: bearer token in URL, structure, unexpected params,
				// credential-config resolution. Each step after the first self-skips when
				// a prior step populated vci.credential_error_response, giving the
				// short-circuit-on-error semantics without imperative bridges.
				// POST method + bearer-in-params are pre-checks that don't consume the
				// c_nonce, so they run unconditionally — even on the DPoP-nonce-error path
				// they log a useful failure without disturbing the wallet's retry. Matches
				// the wallet's credential endpoint at AbstractVCIWalletTest.credentialEndpoint.
				callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.2");
				callAndContinueOnFailure(VCIEnsureBearerAccessTokenNotInParams.class, ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.4-2");
				call(condition(VCIValidateCredentialRequestStructure.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINAL-8.2")
					.dontStopOnFailure());
				call(condition(CheckForUnexpectedParametersInCredentialRequest.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.WARNING)
					.requirements("OID4VCI-1FINAL-8.2")
					.dontStopOnFailure());
				call(condition(VCIResolveRequestedCredentialConfigurationFromRequest.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINAL-8.2")
					.dontStopOnFailure());

				// Proof-binding phase: only fires when the resolved credential configuration
				// requires cryptographic binding (skipIfElementMissing on
				// credential_configuration.cryptographic_binding_methods_supported gates the
				// whole phase). Extract proof, then a proof-type flag condition translates
				// env.proof_type into per-type flag keys (vci.proof_type_<type>); the
				// proof-type-specific conditions then self-gate on their flag.
				call(condition(VCIExtractCredentialRequestProof.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("credential_configuration", "cryptographic_binding_methods_supported")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINALA-F.4")
					.dontStopOnFailure());
				call(condition(VCISetProofTypeFlag.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("credential_configuration", "cryptographic_binding_methods_supported")
					.onFail(ConditionResult.FAILURE)
					.dontStopOnFailure());
				// proof_type=jwt
				call(condition(VCIValidateCredentialRequestJwtProof.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("vci", "proof_type_jwt")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.4")
					.dontStopOnFailure());
				call(condition(VCIValidateAttestedKeysInKeyAttestationFromJwtProof.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("vci", "proof_type_jwt")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.4")
					.dontStopOnFailure());
				// proof_type=attestation
				call(condition(VCIValidateCredentialRequestAttestationProof.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("vci", "proof_type_attestation")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINALA-F.3", "OID4VCI-1FINALA-F.4", "HAIP-4.5.1")
					.dontStopOnFailure());
				call(condition(ValidateKeyAttestationX5cCertificateChain.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("vci", "proof_type_attestation")
					.onFail(ConditionResult.FAILURE)
					.requirements("HAIP-4.5.1")
					.dontStopOnFailure());
				// proof_type=di_vp
				call(condition(VCIValidateCredentialRequestDiVpProof.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("vci", "proof_type_di_vp")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINALA-F.2", "OID4VCI-1FINALA-F.4")
					.dontStopOnFailure());

				// Creation phase: skip if any prior validation set the error sentinel.
				// VCISetCredentialFormatFlag writes a per-format flag so the creation
				// conditions self-gate on it; only the active format actually fires.
				call(condition(CreateFapiInteractionIdIfNeeded.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE)
					.requirements("FAPI2-IMP-2.2.1"));
				call(condition(VCISetCredentialFormatFlag.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE));
				call(condition(CreateMdocCredentialForVCI.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("vci", "format_mso_mdoc")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINALA-G.1"));
				// SD-JWT — the condition instance differs based on whether the profile
				// supplies additional claims, but the additionalClaims map is resolved at
				// dispatch-build time, so we pick the right constructor here.
				call(condition(new CreateSdJwtCredential(additionalClaims))
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.skipIfElementMissing("vci", "format_sd_jwt")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.3"));

				call(condition(VCICreateCredentialEndpointResponse.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINAL-8.3"));
				call(condition(VCIAddNotificationIdToCredentialEndpointResponse.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINAL-8.3"));
				call(condition(ClearAccessTokenFromRequest.class)
					.skipIfElementPresent("vci", "credential_error_response")
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE));
			}
		};
		return new PathDispatch("Credential endpoint", sequence, m -> {
			// DPoP-nonce-error path: sender-constrain populated
			// resource_endpoint_dpop_nonce_error and the sequence ran
			// CreateResourceEndpointDpopErrorResponse. Return the prepared 401 response
			// (so the wallet retries with the correct nonce) without consuming the
			// c_nonce — c_nonce-consuming validation was skipped via skipIfStringPresent.
			if (m.getEnv().getString("resource_endpoint_dpop_nonce_error") != null) {
				JsonObject body = m.getEnv().getObject("resource_endpoint_response");
				JsonObject headers = m.getEnv().getObject("resource_endpoint_response_headers");
				Integer status = m.getEnv().getInteger("resource_endpoint_response_http_status");
				return ResponseEntity.status(status)
					.headers(headersFromJson(headers))
					.body(body);
			}
			// Error path: validation surfaced a vci.credential_error_response; turn it
			// into a 400 with the prepared body, clear the sentinel so subsequent calls
			// (deferred endpoint etc.) aren't poisoned.
			JsonElement errEl = m.getEnv().getElementFromObject("vci", "credential_error_response");
			if (errEl != null) {
				JsonObject errBody = errEl.getAsJsonObject().getAsJsonObject("body");
				m.getEnv().getObject("vci").remove("credential_error_response");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.contentType(MediaType.APPLICATION_JSON)
					.body(errBody);
			}
			// Happy path: build the 200 response from the prepared env body / headers,
			// schedule the delayed test finish so paired issuer tests can keep talking
			// to us (notification, deferred etc.) before the wallet test marks itself
			// FINISHED.
			JsonObject body = m.getEnv().getObject("credential_endpoint_response");
			JsonObject headers = m.getEnv().getObject("credential_endpoint_response_headers");
			ResponseEntity<JsonObject> response = ResponseEntity.status(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON)
				.headers(headersFromJson(headers))
				.body(body);
			m.scheduleDelayedFinishForAdditionalRequests();
			return response;
		});
	}

	/**
	 * OID4VCI 1.0 Final 11.1 notification endpoint — single-sequence flow: clear stale
	 * error state, run sender-constrain checks (via the helper sequence accessor) +
	 * notification-specific validation, return 204 on success or 400 with the error
	 * body when validation populated {@code vci.notification_error_response} per § 11.3.
	 */
	private PathDispatch buildNotificationDispatch() {
		// Clear any previous notification error response so subsequent calls aren't
		// poisoned. The sequence runs after this build call, so the clear happens before
		// any condition observes the env.
		JsonObject vciAtBuild = module.getEnv().getObject("vci");
		if (vciAtBuild != null) {
			vciAtBuild.remove("notification_error_response");
		}
		ConditionSequence sequence = new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// Full resource-endpoint request validation: sender-constrain proof,
				// access-token validation, FAPI resource headers. Matches the wallet's
				// notification endpoint at AbstractVCIWalletTest.notificationEndpoint.
				call(module.checkResourceEndpointRequestSequence(false));
				// DPoP-nonce-error short-circuit: build the prepared 401 response when
				// sender-constrain populated resource_endpoint_dpop_nonce_error. Skip the
				// notification validation below in that case so the wallet can retry.
				call(condition(CreateResourceEndpointDpopErrorResponse.class)
					.skipIfStringMissing("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE)
					.dontStopOnFailure());
				call(condition(VCIValidateNotificationRequest.class)
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.FAILURE)
					.requirements("OID4VCI-1FINAL-11.1")
					.dontStopOnFailure());
				call(condition(VCICheckForUnknownFieldsInNotificationRequest.class)
					.skipIfStringPresent("resource_endpoint_dpop_nonce_error")
					.onFail(ConditionResult.WARNING)
					.requirements("OID4VCI-1FINAL-11.1")
					.dontStopOnFailure());
			}
		};
		return new PathDispatch("Notification endpoint", sequence, m -> {
			// DPoP-nonce-error path: sender-constrain populated
			// resource_endpoint_dpop_nonce_error and the sequence ran
			// CreateResourceEndpointDpopErrorResponse. Return the prepared 401 response
			// (so the wallet retries with the correct nonce) without consuming the
			// c_nonce — c_nonce-consuming validation was skipped via skipIfStringPresent.
			if (m.getEnv().getString("resource_endpoint_dpop_nonce_error") != null) {
				JsonObject body = m.getEnv().getObject("resource_endpoint_response");
				JsonObject headers = m.getEnv().getObject("resource_endpoint_response_headers");
				Integer status = m.getEnv().getInteger("resource_endpoint_response_http_status");
				return ResponseEntity.status(status)
					.headers(headersFromJson(headers))
					.body(body);
			}
			// Error path: validation surfaced vci.notification_error_response; turn it
			// into a 400 with the prepared body per § 11.3. Clear the sentinel so a
			// subsequent notification request isn't poisoned.
			JsonElement errEl = m.getEnv().getElementFromObject("vci", "notification_error_response");
			if (errEl != null) {
				JsonObject errBody = errEl.getAsJsonObject().getAsJsonObject("body");
				m.getEnv().getObject("vci").remove("notification_error_response");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.contentType(MediaType.APPLICATION_JSON)
					.body(errBody);
			}
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		});
	}

	private String baseUrlWithTrailingSlash() {
		String baseUrl = module.getEnv().getString("base_url");
		if (baseUrl == null || baseUrl.isEmpty()) {
			return "";
		}
		return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
	}

	/**
	 * Convert an env headers JsonObject into Spring {@link org.springframework.http.HttpHeaders}.
	 * Inlined since {@link net.openid.conformance.testmodule.DataUtils#headersFromJson} is a
	 * default interface method and the behavior class doesn't implement DataUtils.
	 */
	private static org.springframework.http.HttpHeaders headersFromJson(JsonObject headerJson) {
		org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
		if (headerJson == null) {
			return headers;
		}
		for (Map.Entry<String, JsonElement> entry : headerJson.entrySet()) {
			JsonElement value = entry.getValue();
			if (value.isJsonPrimitive()) {
				headers.add(entry.getKey(), OIDFJSON.getString(value));
			}
		}
		return headers;
	}
}
