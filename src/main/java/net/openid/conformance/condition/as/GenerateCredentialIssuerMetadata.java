package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateCredentialIssuerMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"base_url", "base_mtls_url"})
	@PostEnvironment(required = "credential_issuer_metadata", strings = { "credential_issuer_metadata_url" })
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");
		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		String credentialEndpointUrl = baseUrl + "credential";

		String metadata = """
			{
				"credential_issuer": "%s",
				"credential_endpoint": "%s",
				"credential_configurations_supported": {
					"ExampleCredential": {
						"format": "jwt_vc"
					}
				}
			}
			""".formatted(baseUrl, credentialEndpointUrl);

		JsonObject metadataJson = JsonParser.parseString(metadata).getAsJsonObject();
		env.putObject("credential_issuer_metadata", metadataJson);

		env.putString("credential_issuer_metadata_url", baseUrl + ".well-known/openid-credential-issuer");

		logSuccess("Created credential issuer metadata", args("credential_issuer", metadataJson));

		return env;

	}

}
