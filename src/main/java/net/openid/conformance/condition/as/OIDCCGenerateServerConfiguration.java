package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfo;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Iterator;

public class OIDCCGenerateServerConfiguration extends GenerateServerConfiguration {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "server", strings = { "issuer", "discoveryUrl" })
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		createBaseConfiguration(env, baseUrl);
		JsonObject server = env.getObject("server");

		server.addProperty("userinfo_endpoint", baseUrl + "userinfo");
		server.addProperty("registration_endpoint", baseUrl + "register");

		addScopesSupported(server);
		addResponseTypes(server);
		addResponseModes(server);
		addTokenEndpointAuthMethodsSupported(server);
		addTokenEndpointAuthSigningAlgValuesSupported(server);

		addGrantTypes(server);
		addClaimsParameterSupported(server);
		addAcrValuesSupported(server);
		addSubjectTypesSupported(server);
		addClaimTypesSupported(server);
		addClaimsSupported(server);

		addIdTokenSigningAlgValuesSupported(server);
		addIdTokenEncryptionAlgValuesSupported(server);
		addIdTokenEncryptionEncValuesSupported(server);

		addRequestObjectSigningAlgValuesSupported(server);
		addRequestObjectEncryptionAlgValuesSupported(server);
		addRequestObjectEncryptionEncValuesSupported(server);

		addUserinfoSigningAlgValuesSupported(server);
		addUserinfoEncryptionAlgValuesSupported(server);
		addUserinfoEncryptionEncValuesSupported(server);

		addAdditionalConfiguration(server, baseUrl);
		// add this as the server configuration
		env.putObject("server", server);
		logSuccess("Generated default server configuration", args("server_configuration", server));
		return env;
	}

	protected void addScopesSupported(JsonObject server) {
		JsonArray scopes = new JsonArray();
		scopes.add("openid");
		scopes.add("phone");
		scopes.add("profile");
		scopes.add("email");
		scopes.add("address");
		scopes.add("offline_access");
		server.add("scopes_supported", scopes);
	}


	protected void addResponseTypes(JsonObject server) {
		JsonArray responseTypes = new JsonArray();
		//response types are intentionally in unusual order
		responseTypes.add("code");
		responseTypes.add("id_token code");
		responseTypes.add("token code id_token");
		responseTypes.add("id_token");
		responseTypes.add("token id_token");
		responseTypes.add("token code");
		responseTypes.add("token");
		server.add("response_types_supported", responseTypes);
	}

	protected void addResponseModes(JsonObject server) {
		JsonArray responseModes = new JsonArray();
		responseModes.add("query");
		responseModes.add("fragment");
		responseModes.add("form_post");
		server.add("response_modes_supported", responseModes);
	}

	protected void addTokenEndpointAuthMethodsSupported(JsonObject server) {
		JsonArray clientAuthTypes = new JsonArray();
		clientAuthTypes.add("client_secret_basic");
		clientAuthTypes.add("client_secret_post");
		clientAuthTypes.add("client_secret_jwt");
		clientAuthTypes.add("private_key_jwt");
		server.add("token_endpoint_auth_methods_supported", clientAuthTypes);
	}

	protected void addTokenEndpointAuthSigningAlgValuesSupported(JsonObject server) {
		JsonArray clientAuthTypes = server.get("token_endpoint_auth_methods_supported").getAsJsonArray();
		JsonArray algValues = new JsonArray();

		for (int i=0; i<clientAuthTypes.size(); i++) {
			String authType = OIDFJSON.getString(clientAuthTypes.get(i));

			if (authType.equals("private_key_jwt")) {
				for (JWSAlgorithm alg : JWSAlgorithm.Family.SIGNATURE) {
					algValues.add(alg.getName());
				}
			}

			if (authType.equals("client_secret_jwt")) {
				for (JWSAlgorithm alg : JWSAlgorithm.Family.HMAC_SHA) {
					algValues.add(alg.getName());
				}
			}
		}

		server.add("token_endpoint_auth_signing_alg_values_supported", algValues);
	}

	//Python suite also always returns urn:ietf:params:oauth:grant-type:jwt-bearer and refresh_token
	//We add refresh_token in OIDCCGenerateServerConfigurationWithRefreshTokenGrantType only for refresh token tests
	protected void addGrantTypes(JsonObject server) {
		JsonArray grantTypes = new JsonArray();
		grantTypes.add("authorization_code");
		grantTypes.add("implicit");
		server.add("grant_types_supported", grantTypes);

	}

	protected void addIdTokenSigningAlgValuesSupported(JsonObject server) {
		JsonArray algValues = new JsonArray();
		algValues.add("none");
		for (JWSAlgorithm alg : JWSAlgorithm.Family.SIGNATURE) {
			algValues.add(alg.getName());
		}
		server.add("id_token_signing_alg_values_supported", algValues);
	}

	protected void addIdTokenEncryptionAlgValuesSupported(JsonObject server) {
		JsonArray algValues = new JsonArray();
		Iterator<JWEAlgorithm> algorithmIterator = JWEAlgorithm.Family.ASYMMETRIC.iterator();
		while(algorithmIterator.hasNext()) {
			JWEAlgorithm alg = algorithmIterator.next();
			algValues.add(alg.getName());
		}
		algorithmIterator = JWEAlgorithm.Family.SYMMETRIC.iterator();
		while(algorithmIterator.hasNext()) {
			JWEAlgorithm alg = algorithmIterator.next();
			algValues.add(alg.getName());
		}
		server.add("id_token_encryption_alg_values_supported", algValues);
	}

	protected void addIdTokenEncryptionEncValuesSupported(JsonObject server) {
		JsonArray encValues = new JsonArray();
		Iterator<EncryptionMethod> encryptionMethodIterator = EncryptionMethod.Family.AES_CBC_HMAC_SHA.iterator();
		while(encryptionMethodIterator.hasNext()) {
			EncryptionMethod alg = encryptionMethodIterator.next();
			encValues.add(alg.getName());
		}
		encryptionMethodIterator = EncryptionMethod.Family.AES_GCM.iterator();
		while(encryptionMethodIterator.hasNext()) {
			EncryptionMethod alg = encryptionMethodIterator.next();
			encValues.add(alg.getName());
		}
		server.add("id_token_encryption_enc_values_supported", encValues);
	}

	protected void addRequestObjectSigningAlgValuesSupported(JsonObject server) {
		JsonArray algValues = new JsonArray();
		algValues.add("none");
		for (JWSAlgorithm alg : JWSAlgorithm.Family.SIGNATURE) {
			algValues.add(alg.getName());
		}
		server.add("request_object_signing_alg_values_supported", algValues);
	}

	protected void addRequestObjectEncryptionAlgValuesSupported(JsonObject server) {
		JsonArray algValues = new JsonArray();
		Iterator<JWEAlgorithm> algorithmIterator = JWEAlgorithm.Family.ASYMMETRIC.iterator();
		while(algorithmIterator.hasNext()) {
			JWEAlgorithm alg = algorithmIterator.next();
			algValues.add(alg.getName());
		}
		algorithmIterator = JWEAlgorithm.Family.SYMMETRIC.iterator();
		while(algorithmIterator.hasNext()) {
			JWEAlgorithm alg = algorithmIterator.next();
			algValues.add(alg.getName());
		}
		server.add("request_object_encryption_alg_values_supported", algValues);
	}

	protected void addRequestObjectEncryptionEncValuesSupported(JsonObject server) {
		JsonArray encValues = new JsonArray();
		Iterator<EncryptionMethod> encryptionMethodIterator = EncryptionMethod.Family.AES_CBC_HMAC_SHA.iterator();
		while(encryptionMethodIterator.hasNext()) {
			EncryptionMethod alg = encryptionMethodIterator.next();
			encValues.add(alg.getName());
		}
		encryptionMethodIterator = EncryptionMethod.Family.AES_GCM.iterator();
		while(encryptionMethodIterator.hasNext()) {
			EncryptionMethod alg = encryptionMethodIterator.next();
			encValues.add(alg.getName());
		}
		server.add("request_object_encryption_enc_values_supported", encValues);
	}

	protected void addClaimsParameterSupported(JsonObject server) {
		server.addProperty("claims_parameter_supported", true);
	}


	protected void addAcrValuesSupported(JsonObject server) {
		JsonArray subjectTypes = new JsonArray();
		subjectTypes.add("PASSWORD");
		server.add("acr_values_supported", subjectTypes);
	}

	protected void addSubjectTypesSupported(JsonObject server) {
		JsonArray subjectTypes = new JsonArray();
		subjectTypes.add("public");
		subjectTypes.add("pairwise");
		server.add("subject_types_supported", subjectTypes);
	}

	protected void addClaimTypesSupported(JsonObject server) {
		JsonArray claimTypes = new JsonArray();
		claimTypes.add("normal");
		claimTypes.add("aggregated");
		claimTypes.add("distributed");
		server.add("claim_types_supported", claimTypes);
	}

	protected void addClaimsSupported(JsonObject server) {
		JsonArray claims = new JsonArray();
		for(String claimName : OIDCCLoadUserInfo.SUPPORTED_CLAIMS) {
			claims.add(claimName);
		}
		server.add("claims_supported", claims);
	}

	protected void addUserinfoSigningAlgValuesSupported(JsonObject server) {
		JsonArray algValues = new JsonArray();
		for (JWSAlgorithm alg : JWSAlgorithm.Family.SIGNATURE) {
			algValues.add(alg.getName());
		}
		server.add("userinfo_signing_alg_values_supported", algValues);
	}

	protected void addUserinfoEncryptionAlgValuesSupported(JsonObject server) {
		JsonArray algValues = new JsonArray();
		Iterator<JWEAlgorithm> algorithmIterator = JWEAlgorithm.Family.ASYMMETRIC.iterator();
		while(algorithmIterator.hasNext()) {
			JWEAlgorithm alg = algorithmIterator.next();
			algValues.add(alg.getName());
		}
		algorithmIterator = JWEAlgorithm.Family.SYMMETRIC.iterator();
		while(algorithmIterator.hasNext()) {
			JWEAlgorithm alg = algorithmIterator.next();
			algValues.add(alg.getName());
		}
		server.add("userinfo_encryption_alg_values_supported", algValues);
	}

	protected void addUserinfoEncryptionEncValuesSupported(JsonObject server) {
		JsonArray encValues = new JsonArray();
		Iterator<EncryptionMethod> encryptionMethodIterator = EncryptionMethod.Family.AES_CBC_HMAC_SHA.iterator();
		while(encryptionMethodIterator.hasNext()) {
			EncryptionMethod alg = encryptionMethodIterator.next();
			encValues.add(alg.getName());
		}
		encryptionMethodIterator = EncryptionMethod.Family.AES_GCM.iterator();
		while(encryptionMethodIterator.hasNext()) {
			EncryptionMethod alg = encryptionMethodIterator.next();
			encValues.add(alg.getName());
		}
		server.add("userinfo_encryption_enc_values_supported", encValues);
	}

	/**
	 * Override in child classes to modify the generated server configuration
	 * This will be called at the end, after the configuration is prepared
	 * @param server
	 */
	protected void addAdditionalConfiguration(JsonObject server, String baseUrl) {

	}
}
