package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
 *   <li>Serves minimal {@code /.well-known/openid-credential-issuer} metadata pointing
 *       back to our base URL as the authorization server. This is enough for a HAIP
 *       wallet to discover the AS and run the FAPI2SP client tests; full credential
 *       issuance flow lives on {@code AbstractVCIWalletTest} for now.</li>
 * </ul>
 */
public class VCIClientProfileBehavior extends FAPI2ClientProfileBehavior {

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
	 * Build a minimal HAIP-compliant credential issuer metadata document. The wallet only
	 * needs {@code authorization_servers} for OAuth-only conformance tests, but we include
	 * a placeholder {@code credential_endpoint} and one entry under
	 * {@code credential_configurations_supported} to satisfy the OID4VCI 1.0 Final REQUIRED
	 * fields.
	 */
	protected JsonObject buildCredentialIssuerMetadata() {
		String baseUrl = baseUrlWithTrailingSlash();

		JsonObject metadata = new JsonObject();
		metadata.addProperty("credential_issuer", baseUrl);
		metadata.addProperty("credential_endpoint", baseUrl + "credential");

		JsonArray authServers = new JsonArray();
		authServers.add(baseUrl);
		metadata.add("authorization_servers", authServers);

		JsonObject configs = new JsonObject();
		JsonObject placeholder = new JsonObject();
		placeholder.addProperty("format", "dc+sd-jwt");
		placeholder.addProperty("vct", "urn:vct:fapi2sp_test_credential");
		placeholder.addProperty("scope", "fapi2sp_test_credential");
		configs.add("fapi2sp_test_credential", placeholder);
		metadata.add("credential_configurations_supported", configs);

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
