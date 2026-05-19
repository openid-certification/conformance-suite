package net.openid.conformance.vci10wallet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TemplateProcessor;

import java.net.URI;
import java.util.Map;

/**
 * Static utility for building OID4VCI 1.0 Final credential issuer metadata. Extracted
 * from {@link AbstractVCIWalletTest} so both the wallet test base and
 * {@code VCIClientProfileBehavior} (used by FAPI2SP client tests under the VCI HAIP
 * profile) can produce identical metadata JSON without code duplication.
 *
 * <p>The builder writes a number of derived URLs into the supplied {@link Environment}
 * for downstream conditions to read:
 * <ul>
 *   <li>{@code credential_issuer} — base URL with trailing slash</li>
 *   <li>{@code credential_issuer_metadata_url} — well-known URL (path-suffix style)</li>
 *   <li>{@code credential_issuer_credential_endpoint_url}</li>
 *   <li>{@code credential_issuer_nonce_endpoint_url}</li>
 *   <li>{@code credential_issuer_deferred_credential_endpoint_url} (only when deferred
 *       issuance is enabled — otherwise removed)</li>
 *   <li>{@code credential_issuer_notification_endpoint_url} (only when notifications
 *       are enabled — otherwise removed)</li>
 * </ul>
 */
public final class VCICredentialIssuerMetadataBuilder {

	private VCICredentialIssuerMetadataBuilder() {
	}

	/**
	 * Configuration for the metadata builder. Mirrors the wallet-state inputs
	 * {@code AbstractVCIWalletTest.getCredentialIssuerMetadata} reads from instance
	 * fields.
	 */
	public static final class Config {
		public final String credentialPath;
		public final String noncePath;
		public final String deferredCredentialPath;
		public final String notificationPath;
		public final boolean useMtlsForResources;
		public final boolean notificationsEnabled;
		public final boolean deferredEnabled;
		public final boolean encryptionEnabled;

		public Config(
			String credentialPath,
			String noncePath,
			String deferredCredentialPath,
			String notificationPath,
			boolean useMtlsForResources,
			boolean notificationsEnabled,
			boolean deferredEnabled,
			boolean encryptionEnabled) {
			this.credentialPath = credentialPath;
			this.noncePath = noncePath;
			this.deferredCredentialPath = deferredCredentialPath;
			this.notificationPath = notificationPath;
			this.useMtlsForResources = useMtlsForResources;
			this.notificationsEnabled = notificationsEnabled;
			this.deferredEnabled = deferredEnabled;
			this.encryptionEnabled = encryptionEnabled;
		}
	}

	/**
	 * Build the credential issuer metadata JSON and store the derived URLs in env.
	 *
	 * @throws IllegalStateException if {@code base_url} or {@code base_mtls_url} env
	 *         strings are missing — callers should wrap in their preferred test-failure
	 *         exception type with appropriate test-id context.
	 */
	public static JsonObject buildCredentialIssuerMetadata(Environment env, Config config) {

		String baseUrl = env.getString("base_url");
		String mtlsBaseUrl = env.getString("base_mtls_url");

		if (baseUrl == null || baseUrl.isEmpty()) {
			throw new IllegalStateException("Base URL is empty");
		}
		if (mtlsBaseUrl == null || mtlsBaseUrl.isEmpty()) {
			throw new IllegalStateException("Base MTLS URL is empty");
		}

		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}
		if (!mtlsBaseUrl.endsWith("/")) {
			mtlsBaseUrl = mtlsBaseUrl + "/";
		}

		String credentialIssuer = baseUrl;
		String resourcesBase = config.useMtlsForResources ? mtlsBaseUrl : baseUrl;
		String credentialEndpointUrl = resourcesBase + config.credentialPath;
		String nonceEndpointUrl = resourcesBase + config.noncePath;
		// Deferred URL is only kept when deferred issuance is enabled; the path may be null
		// for behaviors that don't expose a deferred endpoint at all.
		String deferredCredentialEndpointUrl = config.deferredCredentialPath != null
			? resourcesBase + config.deferredCredentialPath
			: "";
		String notificationEndpointUrl = resourcesBase + config.notificationPath;

		String metadata = TemplateProcessor.process("""
			{
				"credential_issuer": "$(credentialIssuer)",
				"credential_endpoint": "$(credentialEndpoint)",
				"nonce_endpoint": "$(nonceEndpoint)",
				"deferred_credential_endpoint": "$(deferredCredentialEndpoint)",
				"notification_endpoint": "$(notificationEndpoint)",
				"authorization_servers": [ "$(credentialIssuer)" ]
			}
			""", Map.of(
			"credentialIssuer", credentialIssuer,
			"credentialEndpoint", credentialEndpointUrl,
			"nonceEndpoint", nonceEndpointUrl,
			"deferredCredentialEndpoint", deferredCredentialEndpointUrl,
			"notificationEndpoint", notificationEndpointUrl
		));

		String credentialIssuerMetadataUrl = generateWellKnownUrlForPath(credentialIssuer, "openid-credential-issuer");
		env.putString("credential_issuer_metadata_url", credentialIssuerMetadataUrl);

		env.putString("credential_issuer", credentialIssuer);
		env.putString("credential_issuer_nonce_endpoint_url", nonceEndpointUrl);
		env.putString("credential_issuer_credential_endpoint_url", credentialEndpointUrl);
		env.putString("credential_issuer_deferred_credential_endpoint_url", deferredCredentialEndpointUrl);
		env.putString("credential_issuer_notification_endpoint_url", notificationEndpointUrl);

		JsonObject metadataJson = JsonParser.parseString(metadata).getAsJsonObject();

		if (!config.notificationsEnabled) {
			metadataJson.remove("notification_endpoint");
			env.removeNativeValue("credential_issuer_notification_endpoint_url");
		}

		if (!config.deferredEnabled) {
			metadataJson.remove("deferred_credential_endpoint");
			env.removeNativeValue("credential_issuer_deferred_credential_endpoint_url");
		}

		// Add credential response encryption metadata if encryption is enabled
		if (config.encryptionEnabled) {
			JsonObject responseEnc = createResponseEncryptionConfig();
			metadataJson.add("credential_response_encryption", responseEnc);

			// Per OID4VCI 1.0 Final Section 8.2, Credential Request encryption MUST be used when
			// credential_response_encryption is included; advertise the issuer's request encryption
			// JWKS so the wallet can encrypt credential requests to it.
			JsonObject requestEnc = createRequestEncryptionConfig(env);
			metadataJson.add("credential_request_encryption", requestEnc);
		}

		return metadataJson;
	}

	/**
	 * Convert an issuer URL with an optional path suffix into the corresponding
	 * {@code .well-known} URL per RFC 8615 path-suffix style:
	 * {@code https://example.com/issuer-path} →
	 * {@code https://example.com/.well-known/<type>/issuer-path}.
	 */
	public static String generateWellKnownUrlForPath(String issuer, String wellKnownTypePath) {
		URI serverIssuerUri = URI.create(issuer);
		String serverIssuerPath = serverIssuerUri.getPath();
		String wellKnownBaseUrl = serverIssuerUri.getScheme() + "://" + serverIssuerUri.getAuthority() + "/.well-known";
		return wellKnownBaseUrl + "/" + wellKnownTypePath + serverIssuerPath;
	}

	/**
	 * Build the {@code credential_request_encryption} sub-document. Reads
	 * {@code vci.credential_request_encryption_public_jwks} from env (must already be
	 * generated, e.g. via {@code VCIGenerateCredentialRequestEncryptionJwks}).
	 */
	public static JsonObject createRequestEncryptionConfig(Environment env) {
		JsonObject requestEnc = new JsonObject();

		JsonObject publicJwks = (JsonObject) env.getElementFromObject("vci", "credential_request_encryption_public_jwks");
		requestEnc.add("jwks", publicJwks);

		JsonArray encValues = new JsonArray();
		encValues.add("A256GCM");
		encValues.add("A128GCM");
		encValues.add("A256CBC-HS512");
		encValues.add("A128CBC-HS256");
		requestEnc.add("enc_values_supported", encValues);
		requestEnc.addProperty("encryption_required", false);
		return requestEnc;
	}

	/**
	 * Add a {@code credential_configurations_supported} entry to the metadata and build the
	 * {@code credential_configuration_id_scope_map} env object that maps each scope value to
	 * the list of credential configuration ids advertising that scope. Mirrors
	 * {@code AbstractVCIWalletTest.configureSupportedCredentialConfigurations}.
	 */
	public static void configureSupportedCredentialConfigurations(Environment env, JsonObject credentialIssuerMetadata, JsonObject supportedCredentialConfigurations) {
		credentialIssuerMetadata.add("credential_configurations_supported", supportedCredentialConfigurations);

		JsonObject scopeToCredentialConfigsMap = new JsonObject();
		for (String configurationId : supportedCredentialConfigurations.keySet()) {
			JsonObject credentialConfiguration = supportedCredentialConfigurations.getAsJsonObject(configurationId);
			if (credentialConfiguration.has("scope")) {
				String scope = OIDFJSON.getString(credentialConfiguration.get("scope"));
				JsonArray configs = scopeToCredentialConfigsMap.getAsJsonArray(scope);
				if (configs == null) {
					configs = new JsonArray();
				}
				configs.add(configurationId);
				scopeToCredentialConfigsMap.add(scope, configs);
			}
		}

		env.putObject("credential_configuration_id_scope_map", scopeToCredentialConfigsMap);
	}

	/**
	 * Build the {@code credential_response_encryption} sub-document — does not depend
	 * on env state, so caller can invoke directly.
	 */
	public static JsonObject createResponseEncryptionConfig() {
		JsonObject responseEnc = new JsonObject();
		JsonArray algValues = new JsonArray();
		algValues.add("ECDH-ES");
		algValues.add("ECDH-ES+A256KW");
		algValues.add("ECDH-ES+A128KW");
		responseEnc.add("alg_values_supported", algValues);

		JsonArray encValues = new JsonArray();
		encValues.add("A256GCM");
		encValues.add("A128GCM");
		encValues.add("A256CBC-HS512");
		encValues.add("A128CBC-HS256");
		responseEnc.add("enc_values_supported", encValues);
		responseEnc.addProperty("encryption_required", false);
		return responseEnc;
	}
}
