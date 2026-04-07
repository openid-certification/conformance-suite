package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

import java.util.HashSet;
import java.util.Set;

public class ValidateDCQLQuery extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);
		String schemaResource = "json-schemas/oid4vp/dcql_request.json";
		String inputName = "DCQL query";
		return new JsonSchemaValidationInput(inputName, schemaResource, dcql);
	}


	@Override
	@PreEnvironment(required = ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);

		Environment result = super.evaluate(env);
		validateReferencesAndUniqueness(dcql);
		return result;
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult structuralErrors = validationResult.withoutAdditionalPropertiesErrors();
		if (!structuralErrors.isValid()) {
			super.onValidationFailure(env, structuralErrors, input);
		}
	}

	private void validateReferencesAndUniqueness(JsonObject dcql) {
		JsonArray credentials = getOptionalArray(dcql, "credentials");
		if (credentials == null) {
			return;
		}

		Set<String> credentialIds = new HashSet<>();
		for (JsonElement credentialElement : credentials) {
			if (!credentialElement.isJsonObject()) {
				continue;
			}
			JsonObject credential = credentialElement.getAsJsonObject();
			String credentialId = getRequiredString(credential, "id");
			if (!credentialIds.add(credentialId)) {
				throw error("Duplicate credential id in dcql query", args("credential_id", credentialId));
			}

			Set<String> claimIds = new HashSet<>();
			Set<String> claimPaths = new HashSet<>();
			JsonArray claims = getOptionalArray(credential, "claims");
			if (claims != null) {
				for (JsonElement claimElement : claims) {
					if (!claimElement.isJsonObject()) {
						continue;
					}
					JsonObject claim = claimElement.getAsJsonObject();
					JsonElement claimIdElement = claim.get("id");
					if (claimIdElement != null && !claimIdElement.isJsonNull()) {
						String claimId = OIDFJSON.getString(claimIdElement);
						if (!claimIds.add(claimId)) {
							throw error("Duplicate claim id in dcql query", args("credential_id", credentialId, "claim_id", claimId));
						}
					}
					// OID4VP section 6.1: Verifiers MUST NOT point to the same claim more than once
					JsonElement pathElement = claim.get("path");
					if (pathElement != null && pathElement.isJsonArray()) {
						String pathKey = pathElement.toString();
						if (!claimPaths.add(pathKey)) {
							throw error("Duplicate claim path in dcql query — verifiers MUST NOT point to the same claim more than once in a single query",
								args("credential_id", credentialId, "duplicate_path", pathElement));
						}
					}
				}
			}

			JsonArray claimSets = getOptionalArray(credential, "claim_sets");
			if (claimSets != null) {
				if (claimIds.isEmpty()) {
					throw error("claim_sets present but no claim ids defined", args("credential_id", credentialId));
				}
				validateClaimSetReferences(claimSets, claimIds, credentialId);
			}
		}

		JsonArray credentialSets = getOptionalArray(dcql, "credential_sets");
		if (credentialSets != null) {
			validateCredentialSetReferences(credentialSets, credentialIds);
		}
	}

	private void validateClaimSetReferences(JsonArray claimSets, Set<String> claimIds, String credentialId) {
		for (JsonElement claimSetElement : claimSets) {
			if (!claimSetElement.isJsonArray()) {
				continue;
			}
			JsonArray claimSet = claimSetElement.getAsJsonArray();
			for (JsonElement claimIdElement : claimSet) {
				String claimId = OIDFJSON.getString(claimIdElement);
				if (!claimIds.contains(claimId)) {
					throw error("claim_sets references unknown claim id", args("credential_id", credentialId, "claim_id", claimId));
				}
			}
		}
	}

	private void validateCredentialSetReferences(JsonArray credentialSets, Set<String> credentialIds) {
		for (JsonElement credentialSetElement : credentialSets) {
			if (!credentialSetElement.isJsonObject()) {
				continue;
			}
			JsonObject credentialSet = credentialSetElement.getAsJsonObject();
			JsonArray options = getOptionalArray(credentialSet, "options");
			if (options == null) {
				continue;
			}
			for (JsonElement optionElement : options) {
				if (!optionElement.isJsonArray()) {
					continue;
				}
				JsonArray option = optionElement.getAsJsonArray();
				for (JsonElement credentialIdElement : option) {
					String credentialId = OIDFJSON.getString(credentialIdElement);
					if (!credentialIds.contains(credentialId)) {
						throw error("credential_sets references unknown credential id", args("credential_id", credentialId));
					}
				}
			}
		}
	}

	private JsonArray getOptionalArray(JsonObject obj, String property) {
		JsonElement element = obj.get(property);
		if (element == null || !element.isJsonArray()) {
			return null;
		}
		return element.getAsJsonArray();
	}

	private String getRequiredString(JsonObject obj, String property) {
		JsonElement element = obj.get(property);
		if (element == null || element.isJsonNull()) {
			throw error(String.format("Missing required '%s' in dcql query", property));
		}
		return OIDFJSON.getString(element);
	}
}
