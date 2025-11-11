package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class VCIExtractCredentialRequestProof extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialRequestBodyJson = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();

		boolean proofsPresent = credentialRequestBodyJson.has("proofs");

		if (!proofsPresent) {
			throw error("Required proofs element is missing in credential request");
		}

		JsonElement proofsEl = credentialRequestBodyJson.get("proofs");
		log("Found proofs element in credential request", args("proofs", proofsEl));
		JsonObject proofsObject = proofsEl.getAsJsonObject();

		if (proofsObject.has("jwt")) {
			// A JWT [RFC7519] is used for proof of possession.

			String proofType = "jwt";
			JsonElement jwtEl = proofsObject.get(proofType);
			if (jwtEl == null || !jwtEl.isJsonArray()) {
				throw error("Expected array in 'jwt' proof object, but found: " + jwtEl, args("proof_type", proofType, "jwt", jwtEl));
			}

			JsonArray jwtArray = jwtEl.getAsJsonArray();
			if (jwtArray.isEmpty()) {
				throw error("Expected non-empty array in 'jwt' proof object, but found:" + jwtArray, args("proof_type", proofType, "jwt", jwtEl));
			}

			log("Found " + jwtArray.size() + " JWT(s) for 'jwt' proof.", args("jwts", jwtArray));
			// TODO handle multiple jwt proof values
			// for now we select the first item in the array
			String jwtString = OIDFJSON.getString(jwtArray.get(0));
			try {
				JsonObject proofJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString);
				env.putObject("proof_jwt", proofJwt);
				env.putString("proof_type", proofType);
				if (jwtArray.size() > 1) {
					log("Found multiple JWTs in 'jwt' proof, this is currently not supported. We continue by using the first jwt in the list");
				}

				logSuccess("Extracted first 'jwt' proof from credential request", args("proof_jwt", proofJwt));
			} catch (ParseException e) {
				throw error("Parsing of 'jwt' prof failed", e, args("proof_jwt_string", jwtString));
			}

		} else if (proofsObject.has("attestation")) {
			// A JWT [RFC7519] representing a key attestation without using a proof of possession of the cryptographic key material that is being attested.
			String proofType = "attestation";
			// throw error("The conformance tests currently does not support the proof type 'attestation' yet");

			// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-F-3.3
			JsonElement attestationEl = proofsObject.get(proofType);
			if (attestationEl == null || !attestationEl.isJsonArray()) {
				throw error("Expected array in 'attestation' proof object, but found: " + attestationEl, args("proof_type", proofType, "attestation", attestationEl));
			}
			JsonArray attestationArray = attestationEl.getAsJsonArray();
			if (attestationArray.isEmpty()) {
				throw error("Expected non-empty array in 'attestation' proof object, but found: " + attestationEl, args("proof_type", proofType, "attestation", attestationEl));
			}
			if (attestationArray.size() != 1) {
				throw error("Expected attestation array with a single JWT in proof object, but found " + attestationArray.size(), args("proof_type", proofType, "attestation", attestationArray));
			}

			log("Found " + attestationArray.size() + " key attestation(s) for 'attestation' proof.", args("key_attestations", attestationArray));
			// TODO handle multiple jwt proof values
			// for now we select the first item in the array
			String jwtString = OIDFJSON.getString(attestationArray.get(0));
			try {
				// D.1. Key Attestation in JWT format
				// See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-D.1
				JsonObject proofJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString);
				// TODO is this really a proof_jwt, or rather a key_attestation_jwt ?
				env.putObject("proof_attestation", proofJwt);
				env.putString("proof_type", proofType);
				if (attestationArray.size() > 1) {
					log("Found multiple key attestation JWTs in 'attestation' proof, this is currently not supported. We continue by using the first key attestation jwt from the list.");
				}

				// TODO verify this in a separate condition, e.g. VCIVerifyKeyAttestationProof (after this condition)
			} catch (ParseException e) {
				throw error("Parsing SD-JWT credential attestation jwt failed", e, args("proof_jwt_string", jwtString));
			}
		} else if (proofsObject.has("di_vp")) {
			// A W3C Verifiable Presentation object signed using the Data Integrity Proof [VC_Data_Integrity] as defined in [VC_DATA_2.0] or [VC_DATA] is used for proof of possession.
			throw error("The conformance tests currently does not support the proof type 'di_vp' yet");
		} else {
			throw error("Expected to find either 'jwt', 'attestation' or 'di_vp' proof types in proofs element", args("proofs", proofsObject));
		}

		return env;
	}
}
