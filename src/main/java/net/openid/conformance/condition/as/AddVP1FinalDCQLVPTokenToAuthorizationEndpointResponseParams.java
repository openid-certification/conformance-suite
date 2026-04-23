package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateAuthorizationEndpointResponseParams.ENV_KEY, ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY}, strings = "credential")
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);
		JsonElement credentials = dcql.get("credentials");
		if (credentials == null || !credentials.isJsonArray()) {
			throw error("'credentials' within dcql object is missing or not an array",
				args("dcql_query", dcql));
		}
		String id = resolveCredentialId(dcql, credentials.getAsJsonArray());

		String credential = env.getString("credential");

		JsonArray credentialArray = new JsonArray();
		credentialArray.add(credential);

		JsonObject vpToken = new JsonObject();
		vpToken.add(id, credentialArray);

		params.add("vp_token", vpToken);

		logSuccess("Added credential in DCQL 'vp_token' authorization endpoint response parameter", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

	private String resolveCredentialId(JsonObject dcql, JsonArray credentials) {
		if (credentials.size() == 1) {
			return extractCredentialId(credentials.get(0), "First entry");
		}

		JsonArray credentialSets = dcql.getAsJsonArray("credential_sets");
		if (credentialSets == null) {
			throw error("DCQL query contains multiple credentials, but this test helper only supports a single effective credential response unless credential_sets unambiguously selects one required credential",
				args("dcql_query", dcql));
		}

		String requiredCredentialId = null;
		for (JsonElement credentialSetElement : credentialSets) {
			if (!credentialSetElement.isJsonObject()) {
				throw error("Entry in credential_sets is not a JSON object",
					args("credential_set", credentialSetElement, "dcql_query", dcql));
			}
			JsonObject credentialSet = credentialSetElement.getAsJsonObject();
			boolean required = !credentialSet.has("required") || OIDFJSON.getBoolean(credentialSet.get("required"));
			if (!required) {
				continue;
			}

			JsonArray options = credentialSet.getAsJsonArray("options");
			if (options == null || options.size() != 1) {
				throw error("This test helper only supports a single required credential_set option when resolving a single effective credential",
					args("credential_set", credentialSet, "dcql_query", dcql));
			}

			JsonElement optionElement = options.get(0);
			if (!optionElement.isJsonArray()) {
				throw error("Option in credential_sets is not an array",
					args("option", optionElement, "credential_set", credentialSet, "dcql_query", dcql));
			}

			JsonArray option = optionElement.getAsJsonArray();
			if (option.size() != 1) {
				throw error("This test helper only supports a required credential_set option containing exactly one credential id",
					args("option", option, "credential_set", credentialSet, "dcql_query", dcql));
			}

			String credentialId = OIDFJSON.getString(option.get(0));
			if (requiredCredentialId != null && !requiredCredentialId.equals(credentialId)) {
				throw error("DCQL query requires multiple credentials, but this test helper only supports returning one credential",
					args("first_required_credential_id", requiredCredentialId,
						"second_required_credential_id", credentialId,
						"dcql_query", dcql));
			}
			requiredCredentialId = credentialId;
		}

		if (requiredCredentialId == null) {
			throw error("DCQL query contains multiple credentials, but no single required credential could be resolved for this test helper",
				args("dcql_query", dcql));
		}

		return requiredCredentialId;
	}

	private String extractCredentialId(JsonElement credentialRequest, String label) {
		if (!credentialRequest.isJsonObject()) {
			throw error(label + " of 'credentials' array within dcql object is not a JSON object",
				args("credential_request", credentialRequest));
		}
		JsonElement idEl = credentialRequest.getAsJsonObject().get("id");
		if (idEl == null) {
			throw error("'id' within " + label.toLowerCase() + " of 'credentials' array within dcql object is missing",
				args("credential_request", credentialRequest));
		}
		if (!idEl.isJsonPrimitive() || !idEl.getAsJsonPrimitive().isString()) {
			throw error("'id' within " + label.toLowerCase() + " of 'credentials' array within dcql object is not a string",
				args("credential_request", credentialRequest));
		}
		return OIDFJSON.getString(idEl);
	}
}
