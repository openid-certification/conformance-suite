package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreateMdocCredentialForVCI;
import net.openid.conformance.condition.as.CreateSdJwtCredential;
import net.openid.conformance.condition.as.GenerateCredentialNonce;
import net.openid.conformance.condition.as.GenerateCredentialNonceResponse;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
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

import java.util.HashMap;
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
	private static final String DEFERRED_CREDENTIAL_PATH = "deferred_credential";
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
					DEFERRED_CREDENTIAL_PATH,
					NOTIFICATION_PATH,
					/* useMtlsForResources */ module.isMTLSConstrain(),
					/* notificationsEnabled */ true,
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
		module.doExposeEnvString("credential_issuer");
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
			|| DEFERRED_CREDENTIAL_PATH.equals(path)
			|| NOTIFICATION_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificPath(String requestId, String path) {
		if (NONCE_PATH.equals(path)) {
			return handleNonceEndpoint(requestId);
		}
		if (NOTIFICATION_PATH.equals(path)) {
			return handleNotificationEndpoint(requestId);
		}
		// CREDENTIAL_PATH or DEFERRED_CREDENTIAL_PATH
		return handleCredentialEndpoint(requestId);
	}

	/**
	 * OID4VCI 1.0 Final § 7.2 nonce endpoint — generates a fresh c_nonce that the wallet
	 * binds into the credential request proof JWT. Mirrors {@code AbstractVCIWalletTest.nonceEndpoint}.
	 */
	protected ResponseEntity<JsonObject> handleNonceEndpoint(String requestId) {
		Environment env = module.getEnv();
		module.doSetStatus(Status.RUNNING);

		module.doCall(module.doExec().startBlock("Nonce endpoint"));
		module.doCall(module.doExec().mapKey("incoming_request", requestId));

		module.doCallAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.1.1");
		module.doCallAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		module.doCallAndStopOnFailure(GenerateCredentialNonce.class, "OID4VCI-1FINAL-ID-7");
		module.doCallAndStopOnFailure(GenerateCredentialNonceResponse.class, "OID4VCI-1FINAL-7.2");

		module.doCall(module.doExec().unmapKey("incoming_request").endBlock());

		JsonObject body = env.getObject("credential_nonce_response");
		JsonObject headers = env.getObject("credential_nonce_response_headers");

		module.doSetStatus(Status.WAITING);
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.headers(headersFromJson(headers))
			.body(body);
	}

	/**
	 * OID4VCI 1.0 Final § 8 credential endpoint — full validation chain:
	 * access token presence in headers (not URL params per FAPI2-SP-FINAL-5.3.4-2),
	 * sender-constrain check, request structure, credential configuration resolution,
	 * cryptographic-binding proof validation (jwt / attestation / di_vp), credential
	 * creation, response shaping. Mirrors {@code AbstractVCIWalletTest.credentialEndpoint}
	 * minus deferred / encrypted branches (those use {@code AbstractVCIWalletTest}).
	 */
	protected Object handleCredentialEndpoint(String requestId) {
		Environment env = module.getEnv();
		module.doSetStatus(Status.RUNNING);

		module.doCall(module.doExec().startBlock("Credential endpoint"));
		module.doCall(module.doExec().mapKey("incoming_request", requestId));

		// Access token must be in Authorization header, not URL params (FAPI2-SP-FINAL-5.3.4-2).
		ResponseEntity<?> errorResponse = callAndContinueOrReturnVciError(VCIEnsureBearerAccessTokenNotInParams.class, "FAPI2-SP-FINAL-5.3.4-2");
		if (errorResponse != null) {
			return finishCredentialEndpoint(errorResponse);
		}

		// Sender-constrain validation (DPoP proof binding etc.) — uses the inherited helper
		// configured by the FAPI2SenderConstrainMethod @VariantSetup on AFCT.
		module.senderConstrainTokenRequestHelper.checkResourceRequest();

		errorResponse = callAndContinueOrReturnVciError(VCIValidateCredentialRequestStructure.class, "OID4VCI-1FINAL-8.2");
		if (errorResponse != null) {
			return finishCredentialEndpoint(errorResponse);
		}

		module.doCallAndContinueOnFailure(CheckForUnexpectedParametersInCredentialRequest.class, ConditionResult.WARNING, "OID4VCI-1FINAL-8.2");

		errorResponse = callAndContinueOrReturnVciError(VCIResolveRequestedCredentialConfigurationFromRequest.class, "OID4VCI-1FINAL-8.2");
		if (errorResponse != null) {
			return finishCredentialEndpoint(errorResponse);
		}

		JsonObject credentialConfiguration = env.getObject("credential_configuration");
		boolean requiresCryptographicBinding = credentialConfiguration != null
			&& credentialConfiguration.has("cryptographic_binding_methods_supported");

		if (requiresCryptographicBinding) {
			errorResponse = callAndContinueOrReturnVciError(VCIExtractCredentialRequestProof.class, "OID4VCI-1FINALA-F.4");
			if (errorResponse != null) {
				return finishCredentialEndpoint(errorResponse);
			}

			String proofType = env.getString("proof_type");
			if ("jwt".equals(proofType)) {
				errorResponse = callAndContinueOrReturnVciError(VCIValidateCredentialRequestJwtProof.class, "OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.4");
				if (errorResponse == null) {
					errorResponse = callAndContinueOrReturnVciError(VCIValidateAttestedKeysInKeyAttestationFromJwtProof.class, "OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.4");
				}
			} else if ("attestation".equals(proofType)) {
				errorResponse = callAndContinueOrReturnVciError(VCIValidateCredentialRequestAttestationProof.class, "OID4VCI-1FINALA-F.3", "OID4VCI-1FINALA-F.4", "HAIP-4.5.1");
				if (errorResponse == null) {
					errorResponse = callAndContinueOrReturnVciError(ValidateKeyAttestationX5cCertificateChain.class, "HAIP-4.5.1");
				}
			} else if ("di_vp".equals(proofType)) {
				errorResponse = callAndContinueOrReturnVciError(VCIValidateCredentialRequestDiVpProof.class, "OID4VCI-1FINALA-F.2", "OID4VCI-1FINALA-F.4");
			}
			if (errorResponse != null) {
				return finishCredentialEndpoint(errorResponse);
			}
		}

		module.doCallAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.2.1");

		// Create the credential — format derived from the resolved credential_configuration.
		String requestedFormat = OIDFJSON.getString(credentialConfiguration.get("format"));
		if ("mso_mdoc".equals(requestedFormat)) {
			module.doCallAndStopOnFailure(CreateMdocCredentialForVCI.class, "OID4VCI-1FINALA-G.1");
		} else {
			// SD-JWT VC (dc+sd-jwt or default). For HAIP, include the status list claim.
			if (module.profile == FAPI2FinalOPProfile.VCI_HAIP) {
				module.doCallAndStopOnFailure(new CreateSdJwtCredential(buildHaipAdditionalSdJwtClaims()),
					"OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.3");
			} else {
				module.doCallAndStopOnFailure(CreateSdJwtCredential.class,
					"OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.3");
			}
		}

		// Immediate response only — deferred / encrypted are AbstractVCIWalletTest's job.
		module.doCallAndStopOnFailure(VCICreateCredentialEndpointResponse.class,
			"OID4VCI-1FINALA-A.3.4", "OID4VCI-1FINALA-A.2.4");
		module.doCallAndStopOnFailure(VCIAddNotificationIdToCredentialEndpointResponse.class, "OID4VCI-1FINAL-8.3");

		module.doCallAndStopOnFailure(ClearAccessTokenFromRequest.class);

		JsonObject body = env.getObject("credential_endpoint_response");
		JsonObject headers = env.getObject("credential_endpoint_response_headers");
		ResponseEntity<JsonObject> response = ResponseEntity.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.headers(headersFromJson(headers))
			.body(body);

		// Schedule delayed test finish so paired issuer tests can hit notification etc.
		// before the wallet test marks itself FINISHED.
		module.scheduleDelayedFinishForAdditionalRequests();
		return finishCredentialEndpoint(response);
	}

	private Object finishCredentialEndpoint(ResponseEntity<?> response) {
		module.doCall(module.doExec().unmapKey("incoming_request").endBlock());
		module.doSetStatus(Status.WAITING);
		return response;
	}

	/**
	 * Wraps {@code callAndContinueOnFailure} with the wallet's
	 * {@code callAndContinueOnFailureOrReturnErrorResponse} pattern: if the condition
	 * populated {@code vci.credential_error_response}, return a 400 ResponseEntity
	 * carrying the error body; otherwise return null and let the caller continue.
	 */
	private ResponseEntity<JsonObject> callAndContinueOrReturnVciError(Class<? extends AbstractCondition> conditionClass, String... requirements) {
		module.doCallAndContinueOnFailure(conditionClass, ConditionResult.FAILURE, requirements);
		JsonElement errEl = module.getEnv().getElementFromObject("vci", "credential_error_response");
		if (errEl == null) {
			return null;
		}
		JsonObject errBody = errEl.getAsJsonObject().getAsJsonObject("body");
		// Clear so subsequent calls aren't mis-attributed to this condition.
		module.getEnv().getObject("vci").remove("credential_error_response");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.contentType(MediaType.APPLICATION_JSON)
			.body(errBody);
	}

	/**
	 * Build the HAIP-required SD-JWT additional claims (status list reference). Mirrors
	 * {@code AbstractVCIWalletTest.additionalSdJwtClaimsForHaip} so paired tests get the
	 * same SD-JWT shape regardless of which test serves the issuer side.
	 */
	private Map<String, Object> buildHaipAdditionalSdJwtClaims() {
		String issuer = module.getEnv().getString("server", "issuer");
		String statusListUri = (issuer == null ? "" : issuer) + "statuslists/1";

		Map<Object, Object> statusListEntry = new HashMap<>();
		statusListEntry.put("idx", 0);
		statusListEntry.put("uri", statusListUri);

		Map<Object, Object> status = new HashMap<>();
		status.put("status_list", statusListEntry);

		Map<String, Object> additionalClaims = new HashMap<>();
		additionalClaims.put("status", status);
		return additionalClaims;
	}

	/**
	 * OID4VCI 1.0 Final § 10.2 notification endpoint — validates the notification body
	 * and returns 204 No Content.
	 */
	protected ResponseEntity<Void> handleNotificationEndpoint(String requestId) {
		Environment env = module.getEnv();
		module.doSetStatus(Status.RUNNING);

		module.doCall(module.doExec().startBlock("Notification endpoint"));
		module.doCall(module.doExec().mapKey("incoming_request", requestId));

		// Clear any previous notification error response so subsequent calls aren't poisoned.
		JsonObject vci = env.getObject("vci");
		if (vci != null) {
			vci.remove("notification_error_response");
		}

		module.senderConstrainTokenRequestHelper.checkResourceRequest();

		module.doCallAndContinueOnFailure(VCIValidateNotificationRequest.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-10.2");
		module.doCallAndContinueOnFailure(VCICheckForUnknownFieldsInNotificationRequest.class, ConditionResult.WARNING, "OID4VCI-1FINAL-10.2");

		module.doCall(module.doExec().unmapKey("incoming_request").endBlock());
		module.doSetStatus(Status.WAITING);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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
