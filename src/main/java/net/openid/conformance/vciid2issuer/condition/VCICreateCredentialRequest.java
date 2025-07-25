package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class VCICreateCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "vci"})
	public Environment evaluate(Environment env) {

		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2
		String credentialConfigId = env.getString("config", "vci.credential_configuration_id");

		JsonObject credentialRequest = new JsonObject();

		JsonElement authorizationDetailsEl = env.getElementFromObject("token_endpoint_response", "authorization_details");

		if (authorizationDetailsEl != null) {
			// see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2-2.1

			JsonArray authorizationDetails = authorizationDetailsEl.getAsJsonArray();
			log("Inspecting authorization details for a credential identifier", args("authorization_details", authorizationDetails));

			for (JsonElement authorizationDetailEntry : authorizationDetails) {
				JsonObject authorizationDetailEntryObject = authorizationDetailEntry.getAsJsonObject();
				if ("openid_credential".equals(OIDFJSON.getString(authorizationDetailEntryObject.get("type")))
					&& credentialConfigId.equals(OIDFJSON.getString(authorizationDetailEntryObject.get("credential_configuration_id")))
					&& authorizationDetailEntryObject.has("credential_identifiers")
				) {
					JsonArray credentialIdentifiers = authorizationDetailEntryObject.getAsJsonArray("credential_identifiers");
					// we take the first identifier here
					String firstIdentifier = OIDFJSON.getString(credentialIdentifiers.get(0));
					credentialRequest.addProperty("credential_identifier", firstIdentifier);
					log("Adding credential identifier to credential request", args("credential_identifier", firstIdentifier));
					break;
				}
			}

			if (!credentialRequest.has("credential_identifier")) {
				throw error("Couldn't find credential identifier in authorization details", args("credential_configuration_id", credentialConfigId, "authorization_details", authorizationDetails));
			}

		} else {
			// see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2-2.2

			log("No authorization details found, continue with credential_configuration_id", args("credential_configuration_id", credentialConfigId));
			credentialRequest.addProperty("credential_configuration_id", credentialConfigId);
		}

		addProofInformation(env, credentialRequest);

		String credentialRequestJson = credentialRequest.toString();
		env.putString("resource_request_entity", credentialRequestJson);

		log("Created credential request", args("credential_request", credentialRequest));

		return env;
	}

	protected void addProofInformation(Environment env, JsonObject credentialRequest) {
		// Note that proof is no longer part of draft 16
		JsonObject proofObject = createProofObject(env);
		credentialRequest.add("proof", proofObject);

		// see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2-2.4
//		JsonObject proofsObject = createProofsObject(env);
//		credentialRequest.add("proofs", proofsObject);
	}

	protected JsonObject createProofsObject(Environment env) {

		JsonObject proofsObject = new JsonObject();
		String credentialProofJwt = env.getString("vci", "proof.jwt");

		proofsObject.add("jwt", OIDFJSON.convertListToJsonArray(List.of(credentialProofJwt)));
		return proofsObject;
	}

	protected JsonObject createProofObject(Environment env) {

		String credentialProofJwt = env.getString("vci", "proof.jwt");

		JsonObject proofObject = new JsonObject();
		proofObject.addProperty("proof_type", "jwt");
		proofObject.addProperty("jwt", credentialProofJwt);
		return proofObject;
	}
}
