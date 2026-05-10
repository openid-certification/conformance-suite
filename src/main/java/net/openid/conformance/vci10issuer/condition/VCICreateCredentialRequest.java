package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCICreateCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "vci", "client"})
	public Environment evaluate(Environment env) {

		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2
		String credentialConfigId = env.getString("vci_credential_configuration_id");

		JsonObject credentialRequest = new JsonObject();

		JsonElement byConfigIdEl = env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY);
		JsonElement identifiersEl = byConfigIdEl != null && byConfigIdEl.isJsonObject()
			? byConfigIdEl.getAsJsonObject().get(credentialConfigId)
			: null;

		if (identifiersEl != null && identifiersEl.isJsonArray()) {
			// see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2-2.1
			JsonArray credentialIdentifiers = identifiersEl.getAsJsonArray();
			if (credentialIdentifiers.isEmpty()) {
				throw error("Recorded credential_identifiers list is empty", args("credential_configuration_id", credentialConfigId));
			}
			// we take the first identifier here
			String firstIdentifier = OIDFJSON.getString(credentialIdentifiers.get(0));
			credentialRequest.addProperty("credential_identifier", firstIdentifier);
			log("Adding credential identifier to credential request",
				args("credential_identifier", firstIdentifier,
					"credential_configuration_id", credentialConfigId,
					"credential_identifiers", credentialIdentifiers));
		} else {
			// see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2-2.2
			log("No credential_identifiers recorded for credential_configuration_id; continuing with credential_configuration_id",
				args("credential_configuration_id", credentialConfigId));
			credentialRequest.addProperty("credential_configuration_id", credentialConfigId);
		}

		addProofsInformation(env, credentialRequest);

		env.putObject("vci_credential_request_object", credentialRequest);

		log("Created credential request", args("credential_request", credentialRequest));

		return env;
	}

	protected void addProofsInformation(Environment env, JsonObject credentialRequest) {
		// Check if the credential configuration requires cryptographic binding
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			log("Credential configuration does not require cryptographic binding, skipping proofs in credential request");
			return;
		}

		// see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2-2.4
		JsonObject proofsObject = createProofsObject(env);
		credentialRequest.add("proofs", proofsObject);
	}

	protected JsonObject createProofsObject(Environment env) {
		return env.getObject("credential_request_proofs");
	}
}
