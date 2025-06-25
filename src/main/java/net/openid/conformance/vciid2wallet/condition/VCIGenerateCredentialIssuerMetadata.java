package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TemplateProcessor;

import java.util.Map;

public class VCIGenerateCredentialIssuerMetadata extends AbstractCondition {

	private final Boolean mtlsConstrain;

	public VCIGenerateCredentialIssuerMetadata() {
		this(false);
	}

	public VCIGenerateCredentialIssuerMetadata(Boolean mtlsConstrain) {
		this.mtlsConstrain = mtlsConstrain;
	}

	@Override
	@PreEnvironment(strings = {"base_url", "base_mtls_url"})
	@PostEnvironment(required = "credential_issuer_metadata", strings = { "credential_issuer_metadata_url" })
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");
		String mtlsBaseUrl = env.getString("base_mtls_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		if (mtlsBaseUrl.isEmpty()) {
			throw error("Base MTLS URL is empty");
		}

		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}
		if (!mtlsBaseUrl.endsWith("/")) {
			mtlsBaseUrl = mtlsBaseUrl + "/";
		}

		String credentialIssuer = baseUrl;
		String credentialEndpointUrl = (mtlsConstrain ? mtlsBaseUrl: baseUrl) + "credential";
		String nonceEndpointUrl = (mtlsConstrain ? mtlsBaseUrl: baseUrl) + "nonce";
		String credentialConfigurationId = "eu.europa.ec.eudi.pid.1";
		String credentialScope = "eudi.pid.1";
		String credentialFormat = "dc+sd-jwt";
		String vct = "urn:eudi:pid:1";

		String metadata = TemplateProcessor.process("""
		{
			"credential_issuer": "$(credentialIssuer)",
			"credential_endpoint": "$(credentialEndpoint)",
			"nonce_endpoint": "$(nonceEndpoint)",
			"credential_configurations_supported": {
				"$(credentialConfigurationId)": {
					"format": "$(credentialFormat)",
					"vct": "$(vct)",
					"scope": "$(scope)",
					"cryptographic_binding_methods_supported": ["jwk"],
					"credential_signing_alg_values_supported": ["ES256"],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": ["ES256"]
						}
					}
				}
			}
		}
		""", Map.of(
			"credentialIssuer", credentialIssuer,
			"credentialEndpoint", credentialEndpointUrl,
			"nonceEndpoint", nonceEndpointUrl,
			"credentialConfigurationId", credentialConfigurationId,
			"credentialFormat", credentialFormat,
			"vct", vct,
			"scope", credentialScope
		));

		JsonObject metadataJson = JsonParser.parseString(metadata).getAsJsonObject();
		env.putObject("credential_issuer_metadata", metadataJson);

		env.putString("credential_issuer", credentialIssuer);
		env.putString("credential_issuer_metadata_url", baseUrl + ".well-known/openid-credential-issuer");
		env.putString("credential_issuer_nonce_endpoint_url", nonceEndpointUrl);
		env.putString("credential_issuer_credential_endpoint_url", credentialEndpointUrl);

		JsonObject scopeToCredentialMap = new JsonObject();
		scopeToCredentialMap.addProperty(credentialScope, credentialConfigurationId);
		env.putObject("credential_issuer_credential_configuration_id_scope_map", scopeToCredentialMap);

		logSuccess("Created credential issuer metadata", args("credential_issuer", metadataJson));

		return env;

	}
}
