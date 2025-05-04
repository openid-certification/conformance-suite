package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateCredentialIssuerMetadata extends AbstractCondition {

	private final Boolean mtlsConstrain;

	public GenerateCredentialIssuerMetadata() {
		this(false);
	}

	public GenerateCredentialIssuerMetadata(Boolean mtlsConstrain) {
		this.mtlsConstrain = mtlsConstrain;
	}

	@Override
	@PreEnvironment(strings = {"base_url", "base_mtls_url"})
	@PostEnvironment(required = "credential_issuer_metadata", strings = { "credential_issuer_metadata_url" })
	public Environment evaluate(Environment env) {

		String baseUrl = getBaseUrl(env);
		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		String credentialEndpointUrl = baseUrl + "credential";

		String nonceEndpointUrl = baseUrl + "nonce";

		String metadata = """
		{
			"credential_issuer": "%s",
			"credential_endpoint": "%s",
			"nonce_endpoint": "%s",
			"credential_configurations_supported": {
				"ExampleCredential": {
					"format": "jwt_vc"
				}
			}
		}
		""".formatted(env.getString("base_url"), credentialEndpointUrl, nonceEndpointUrl);

		JsonObject metadataJson = JsonParser.parseString(metadata).getAsJsonObject();
		env.putObject("credential_issuer_metadata", metadataJson);

		env.putString("credential_issuer_metadata_url", baseUrl + ".well-known/openid-credential-issuer");
		env.putString("credential_issuer_nonce_endpoint_url", nonceEndpointUrl);
		env.putString("credential_issuer_credential_endpoint_url", credentialEndpointUrl);

		logSuccess("Created credential issuer metadata", args("credential_issuer", metadataJson));

		return env;

	}

	protected String getBaseUrl(Environment env) {
		return mtlsConstrain ? env.getString("base_mtls_url") : env.getString("base_url");
	}

}
