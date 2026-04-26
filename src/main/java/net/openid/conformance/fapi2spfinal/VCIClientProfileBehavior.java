package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.vci10wallet.VCICredentialConfigurations;
import net.openid.conformance.vci10wallet.condition.clientattestation.AddClientAttestationSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterClientAttestationTrustAnchor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Profile behavior for VCI (Verifiable Credentials Issuance) client tests.
 *
 * <p>The conformance suite acts as the AS that the wallet under test interacts with.
 * For the FAPI2SP client tests pulled into the VCI wallet plan, the AS-emulator
 * defaults from {@link FAPI2ClientProfileBehavior} are appropriate — the wallet's
 * VCI-specific behavior (credential offer, credential / nonce / deferred / notification
 * endpoints) is exercised by the existing {@code VCIWalletTest*} modules, not by the
 * FAPI2SP client tests.
 *
 * <p>What this behavior <em>does</em> add over the defaults:
 * <ul>
 *   <li>Exposes {@code credential_issuer} to the user so a HAIP wallet can be configured
 *       with the credential issuer URL (the same as the test base URL).</li>
 *   <li>Serves {@code /.well-known/openid-credential-issuer} metadata pointing back to our
 *       base URL as the authorization server, with the same default credential
 *       configurations as {@link net.openid.conformance.vci10wallet.AbstractVCIWalletTest}
 *       (so wallets configured for any of the standard SD-JWT or mdoc credential types
 *       can resolve their requested credential_configuration_id).</li>
 * </ul>
 *
 * <p>The credential / nonce / deferred / notification endpoints are advertised but not
 * implemented here — the FAPI2SP client tests don't exercise them. End-to-end credential
 * issuance for HAIP wallets uses {@code AbstractVCIWalletTest}'s own modules.
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

	/**
	 * Build credential issuer metadata for the FAPI2SP-client-test side of the VCI HAIP
	 * wallet plan. Uses the same default {@code credential_configurations_supported} as
	 * {@link AbstractVCIWalletTest} so wallets configured for any of the standard
	 * SD-JWT VC or mdoc credential types resolve correctly.
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

	private String baseUrlWithTrailingSlash() {
		String baseUrl = module.getEnv().getString("base_url");
		if (baseUrl == null || baseUrl.isEmpty()) {
			return "";
		}
		return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
	}
}
