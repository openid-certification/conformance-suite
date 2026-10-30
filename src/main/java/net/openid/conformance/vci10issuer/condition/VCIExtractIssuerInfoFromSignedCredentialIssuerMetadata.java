package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Extracts {@code issuer_info} (ETSI TS 119 472-3 clause 4.2.3) from the top level of the payload of
 * the signed credential issuer metadata, wrapped as {@code {"issuer_info": [...]}} so that the schema
 * checks can validate it.
 */
public class VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata extends AbstractCondition {

	public static final String ENV_KEY = "issuer_info";

	@Override
	@PreEnvironment(required = "vci")
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonElement signed = env.getElementFromObject("vci", "credential_issuer_metadata_signed");
		if (signed == null || !signed.isJsonObject()) {
			throw error("No signed credential issuer metadata to take 'issuer_info' from");
		}
		JsonElement issuerInfo = signed.getAsJsonObject().get("issuer_info");
		if (issuerInfo == null) {
			throw error("The payload of the signed credential issuer metadata has no 'issuer_info' parameter at its top level;"
					+ " a PID or EAA provider must provide its registration information in it",
				args("credential_issuer_metadata_signed", signed));
		}
		if (!issuerInfo.isJsonArray()) {
			throw error("'issuer_info' in the signed credential issuer metadata is not an array",
				args("issuer_info", issuerInfo));
		}

		JsonObject wrapper = new JsonObject();
		wrapper.add("issuer_info", issuerInfo);
		env.putObject(ENV_KEY, wrapper);
		logSuccess("Found 'issuer_info' at the top level of the signed credential issuer metadata",
			args("issuer_info", issuerInfo));
		return env;
	}
}
