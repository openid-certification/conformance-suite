package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.vci10wallet.VCICredentialConfigurations;
import net.openid.conformance.vci10wallet.condition.clientattestation.AddClientAttestationSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterClientAttestationTrustAnchor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Profile behavior for VCI (Verifiable Credentials Issuance) client tests.
 *
 * <p>The conformance suite acts as the AS that the wallet under test interacts with.
 * For the FAPI2SP client tests pulled into the VCI wallet plan, the AS-emulator
 * defaults from {@link FAPI2ClientProfileBehavior} are appropriate — the wallet's
 * VCI-specific behavior (credential offer, full credential issuance flow including
 * proof / key attestation) is exercised by the existing {@code VCIWalletTest*}
 * modules, not by the FAPI2SP client tests.
 *
 * <p>What this behavior <em>does</em> add over the defaults:
 * <ul>
 *   <li>Exposes {@code credential_issuer} to the user so a HAIP wallet can be configured
 *       with the credential issuer URL (the same as the test base URL).</li>
 *   <li>Serves {@code /.well-known/openid-credential-issuer} metadata pointing back to our
 *       base URL as the authorization server, with the same default credential
 *       configurations as {@link net.openid.conformance.vci10wallet.AbstractVCIWalletTest}.</li>
 *   <li>Implements minimal {@code /nonce} and {@code /credential} endpoint stubs so a
 *       HAIP wallet that follows through after a successful OAuth flow can complete its
 *       discovery and credential request without aborting the test before our
 *       OAuth-level checks finish. The credential endpoint marks the test complete on
 *       first call.</li>
 * </ul>
 *
 * <p>Optional VCI metadata endpoints ({@code nonce_endpoint},
 * {@code deferred_credential_endpoint}, {@code notification_endpoint}) are intentionally
 * not advertised — wallets that respect "advertise only what you implement" skip them,
 * and wallets that probe anyway are handled by the in-place stubs above. End-to-end
 * credential issuance for HAIP wallets is the responsibility of the {@code VCIWalletTest*}
 * modules.
 */
public class VCIClientProfileBehavior extends FAPI2ClientProfileBehavior {

	private static final String CREDENTIAL_PATH = "credential";
	private static final String NONCE_PATH = "nonce";
	private static final String DEFERRED_CREDENTIAL_PATH = "deferred_credential";
	private static final String NOTIFICATION_PATH = "notification";

	/** Seconds to wait for additional issuer-test requests after the credential endpoint
	 * has been called before firing test finished. */
	private static final long DELAYED_FINISH_SECONDS = 10;

	/** Tracks whether the delayed finish has already been scheduled, so repeat calls to
	 * the credential endpoint don't pile up tasks. */
	private final AtomicBoolean finishScheduled = new AtomicBoolean(false);

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

		// Validate the wallet test config has the VCI client-attestation fields populated.
		// The fields are declared via @VariantConfigurationFields on AbstractFAPI2SPFinalClientTest
		// so the schedule-test UI prompts for them; this catches the case where they're left blank.
		if (module.getEnv().getString("config", "vci.client_attestation_issuer") == null) {
			throw new TestFailureException(module.getId(),
				"'Client attestation issuer' field is missing from the 'VCI' section in the test configuration");
		}
		if (module.getEnv().getString("config", "vci.client_attestation_trust_anchor") == null) {
			throw new TestFailureException(module.getId(),
				"'Client attestation trust anchor' field is missing from the 'VCI' section in the test configuration");
		}

		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddClientAttestationSigningAlgValuesSupportedToServerConfiguration.class, "OAuth2-ATCA07-10.1");
				callAndStopOnFailure(VCIRegisterClientAttestationTrustAnchor.class);
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
			return ResponseEntity.status(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON)
				.body(buildCredentialIssuerMetadata());
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
			return handleNonceEndpoint();
		}
		if (NOTIFICATION_PATH.equals(path)) {
			return handleNotificationEndpoint();
		}
		// CREDENTIAL_PATH or DEFERRED_CREDENTIAL_PATH
		return handleCredentialEndpoint();
	}

	/**
	 * Build credential issuer metadata for the FAPI2SP-client-test side of the VCI HAIP
	 * wallet plan. Uses the same default {@code credential_configurations_supported} as
	 * {@link AbstractVCIWalletTest} so wallets configured for any of the standard
	 * SD-JWT VC or mdoc credential types resolve correctly. Advertises all the standard
	 * VCI endpoints (credential, nonce, deferred_credential, notification) so that issuer
	 * tests paired against this wallet can exercise the full VCI surface — minimal
	 * handlers below let those calls succeed.
	 */
	protected JsonObject buildCredentialIssuerMetadata() {
		String baseUrl = baseUrlWithTrailingSlash();

		JsonObject metadata = new JsonObject();
		metadata.addProperty("credential_issuer", baseUrl);
		metadata.addProperty("credential_endpoint", baseUrl + CREDENTIAL_PATH);
		metadata.addProperty("nonce_endpoint", baseUrl + NONCE_PATH);
		metadata.addProperty("deferred_credential_endpoint", baseUrl + DEFERRED_CREDENTIAL_PATH);
		metadata.addProperty("notification_endpoint", baseUrl + NOTIFICATION_PATH);

		JsonArray authServers = new JsonArray();
		authServers.add(baseUrl);
		metadata.add("authorization_servers", authServers);

		metadata.add("credential_configurations_supported",
			VCICredentialConfigurations.getDefault(module.getId()));

		return metadata;
	}

	/**
	 * Minimal nonce endpoint — returns a fresh c_nonce. The FAPI2SP client tests do not
	 * advertise nonce_endpoint, but a wallet that calls it anyway gets a sensible
	 * response so the OAuth-level test continues without aborting.
	 */
	protected ResponseEntity<JsonObject> handleNonceEndpoint() {
		module.doSetStatus(Status.RUNNING);
		JsonObject body = new JsonObject();
		body.addProperty("c_nonce", randomNonce());
		module.doSetStatus(Status.WAITING);
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(body);
	}

	/**
	 * Minimal credential endpoint (also used for deferred_credential) — returns a
	 * placeholder credential response in the OID4VCI 1.0 Final § 8.3 shape
	 * ({@code {"credentials": [{"credential": "<value>"}]}}) and schedules a delayed test
	 * completion (so subsequent issuer-test calls to e.g. notification can still complete
	 * before the wallet test fires {@code FINISHED}). The FAPI2SP client tests don't
	 * implement actual credential issuance — the placeholder will fail issuer-side
	 * format validation (e.g. {@code ParseMdocCredentialFromVCIIssuance}), which is
	 * expected: the wallet-side OAuth checks have already passed by this point. For
	 * end-to-end credential issuance against an issuer test, use the
	 * {@code oid4vci-1_0-wallet-test-credential-issuance} module instead.
	 */
	protected ResponseEntity<JsonObject> handleCredentialEndpoint() {
		module.doSetStatus(Status.RUNNING);
		JsonObject credentialEntry = new JsonObject();
		credentialEntry.addProperty("credential", "fapi2sp-client-test-placeholder-credential");
		JsonArray credentials = new JsonArray();
		credentials.add(credentialEntry);
		JsonObject body = new JsonObject();
		body.add("credentials", credentials);
		scheduleDelayedTestFinish();
		module.doSetStatus(Status.WAITING);
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(body);
	}

	/**
	 * Minimal notification endpoint — accepts the notification and returns 204 No Content
	 * per OID4VCI 1.0 Final § 10.2.
	 */
	protected ResponseEntity<Void> handleNotificationEndpoint() {
		module.doSetStatus(Status.RUNNING);
		module.doSetStatus(Status.WAITING);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	/**
	 * Schedule a one-shot background task that fires test finished after a short delay.
	 * Lets paired issuer tests continue to probe other endpoints (notification, etc.)
	 * after the credential endpoint without {@code resourceEndpointCallComplete}'s
	 * immediate FINISHED state breaking subsequent {@code setStatus(RUNNING)} calls.
	 */
	private void scheduleDelayedTestFinish() {
		if (!finishScheduled.compareAndSet(false, true)) {
			return;
		}
		module.getTestExecutionManager().scheduleInBackground(() -> {
			module.doSetStatus(Status.RUNNING);
			module.fireTestFinished();
			return null;
		}, DELAYED_FINISH_SECONDS, TimeUnit.SECONDS);
	}

	private static String randomNonce() {
		byte[] bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String baseUrlWithTrailingSlash() {
		String baseUrl = module.getEnv().getString("base_url");
		if (baseUrl == null || baseUrl.isEmpty()) {
			return "";
		}
		return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
	}
}
