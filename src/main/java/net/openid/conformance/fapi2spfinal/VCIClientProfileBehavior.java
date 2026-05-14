package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreateMdocCredentialForVCI;
import net.openid.conformance.condition.as.CreateSdJwtCredential;
import net.openid.conformance.condition.as.GenerateCredentialNonce;
import net.openid.conformance.condition.as.GenerateCredentialNonceResponse;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.CreateResourceEndpointDpopErrorResponse;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.vci10wallet.VCICredentialConfigurations;
import net.openid.conformance.vci10wallet.VCICredentialIssuerMetadataBuilder;
import net.openid.conformance.vci10wallet.condition.CheckForUnexpectedParametersInCredentialRequest;
import net.openid.conformance.vci10wallet.condition.VCIAddNotificationIdToCredentialEndpointResponse;
import net.openid.conformance.vci10wallet.condition.VCICheckForUnknownFieldsInNotificationRequest;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialEndpointResponse;
import net.openid.conformance.vci10wallet.condition.VCIEnsureBearerAccessTokenNotInParams;
import net.openid.conformance.vci10wallet.condition.VCIEnsureCredentialSigningCertificateIsNotSelfSigned;
import net.openid.conformance.vci10wallet.condition.VCIExtractCredentialRequestProof;
import net.openid.conformance.vci10wallet.condition.VCIResolveRequestedCredentialConfigurationFromRequest;
import net.openid.conformance.vci10wallet.condition.VCISetCredentialFormatFlag;
import net.openid.conformance.vci10wallet.condition.VCISetProofTypeFlag;
import net.openid.conformance.vci10wallet.condition.VCIValidateAttestedKeysInKeyAttestationFromJwtProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestAttestationProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestDiVpProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestJwtProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestStructure;
import net.openid.conformance.vci10wallet.condition.VCIValidateNotificationRequest;
import net.openid.conformance.vci10wallet.condition.ValidateKeyAttestationX5cCertificateChain;
import net.openid.conformance.vci10wallet.condition.clientattestation.AddClientAttestationSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterClientAttestationTrustAnchor;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterKeyAttestationTrustAnchor;
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
 * </ul>
 *
 * <p>End-to-end credential issuance with encryption / deferred issuance lives on
 * {@code AbstractVCIWalletTest}'s own modules ({@code oid4vci-1_0-wallet-test-credential-issuance}).
 */
public class VCIClientProfileBehavior extends FAPI2ClientProfileBehavior {

	private static final String CREDENTIAL_PATH = "credential";
	private static final String NONCE_PATH = "nonce";
	private static final String NOTIFICATION_PATH = "notification";

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
		if (module.clientAuthType != ClientAuthType.CLIENT_ATTESTATION) {
			return null;
		}

		Environment env = module.getEnv();

		// Validate the wallet test config has the VCI client-attestation fields populated.
		// The fields are declared via @VariantConfigurationFields on AbstractFAPI2SPFinalClientTest
		// so the schedule-test UI prompts for them; this catches the case where they're left blank.
		if (env.getString("config", "vci.client_attestation_issuer") == null) {
			throw new TestFailureException(module.getId(),
				"'Client attestation issuer' field is missing from the 'VCI' section in the test configuration");
		}
		if (env.getString("config", "vci.client_attestation_trust_anchor") == null) {
			throw new TestFailureException(module.getId(),
				"'Client attestation trust anchor' field is missing from the 'VCI' section in the test configuration");
		}

		// The credential signing JWK is required so we can issue real mdoc / SD-JWT credentials.
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (credentialSigningJwkEl == null) {
			throw new TestFailureException(module.getId(),
				"'Credential Signing JWK' field is missing from the 'Credential' section in the test configuration");
		}
		env.putString("vci", "credential_signing_jwk", credentialSigningJwkEl.toString());

		// Pre-populate credential_issuer_metadata + credential_configurations_supported
		// so the VCI conditions invoked from the credential / nonce endpoints find them.
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
					/* encryptionEnabled */ false));
		} catch (IllegalStateException e) {
			throw new TestFailureException(module.getId(), e.getMessage());
		}
		env.putObject("credential_issuer_metadata", metadata);
		VCICredentialIssuerMetadataBuilder.configureSupportedCredentialConfigurations(env, metadata,
			VCICredentialConfigurations.getDefault(module.getId()));

		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddClientAttestationSigningAlgValuesSupportedToServerConfiguration.class, "OAuth2-ATCA07-10.1");
				callAndStopOnFailure(VCIRegisterClientAttestationTrustAnchor.class);
				callAndStopOnFailure(VCIEnsureCredentialSigningCertificateIsNotSelfSigned.class, "HAIP-4.1");
				callAndStopOnFailure(VCIRegisterKeyAttestationTrustAnchor.class);
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
			|| NOTIFICATION_PATH.equals(path);
	}

	@Override
	public PathDispatch getProfileSpecificPathDispatch(String requestId, String path) {
		if (NONCE_PATH.equals(path)) {
			return buildNonceDispatch(requestId);
		}
		if (NOTIFICATION_PATH.equals(path)) {
			return buildNotificationDispatch(requestId);
		}
		// CREDENTIAL_PATH
		return buildCredentialDispatch(requestId);
	}

	/**
	 * Additional SD-JWT claims to inject into the issued credential. Default is none
	 * ({@code null}). Subclasses for profiles that require specific SD-JWT claims (e.g.
	 * the HAIP status_list reference, see {@link VCIHaipClientProfileBehavior}) override
	 * this to return a populated map.
	 */
	protected Map<String, Object> additionalSdJwtClaims() {
		return null;
	}

	/**
	 * OID4VCI 1.0 Final 7.2 nonce endpoint — generates a fresh c_nonce that the wallet
	 * binds into the credential request proof JWT. Single-sequence dispatch: condition
	 * sequence does the validation + generation; response builder reads the prepared
	 * response from env. Mirrors AbstractVCIWalletTest.nonceEndpoint.
	 */
	private PathDispatch buildNonceDispatch(String requestId) {
		ConditionSequence sequence = new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(exec().startBlock("Nonce endpoint"));
				call(exec().mapKey("incoming_request", requestId));
				callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.1.1");
				callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.1");
				callAndStopOnFailure(GenerateCredentialNonce.class, "OID4VCI-1FINAL-7");
				callAndStopOnFailure(GenerateCredentialNonceResponse.class, "OID4VCI-1FINAL-7.2");
				call(exec().unmapKey("incoming_request").endBlock());
			}
		};
		return new PathDispatch(sequence, m -> {
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
	private PathDispatch buildCredentialDispatch(String requestId) {
		final Map<String, Object> additionalClaims = additionalSdJwtClaims();
		ConditionSequence sequence = new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(exec().startBlock("Credential endpoint"));
				call(exec().mapKey("incoming_request", requestId));

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

				call(exec().unmapKey("incoming_request").endBlock());
			}
		};
		return new PathDispatch(sequence, m -> {
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
	private PathDispatch buildNotificationDispatch(String requestId) {
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
				call(exec().startBlock("Notification endpoint"));
				call(exec().mapKey("incoming_request", requestId));
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
				call(exec().unmapKey("incoming_request").endBlock());
			}
		};
		return new PathDispatch(sequence, m -> {
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
