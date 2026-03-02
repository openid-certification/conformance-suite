package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks whether the issuer applied DEFLATE compression to the encrypted credential response
 * when the request included "zip": "DEF".
 *
 * If the issuer's metadata advertises "DEF" in zip_values_supported but the JWE response
 * header does not contain a "zip" field, this is flagged as an issue.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2">OID4VCI Section 8.2 - Credential Request</a>
 */
public class VCICheckCredentialResponseCompression extends AbstractCondition {

	@Override
	@PreEnvironment(required = "credential_response_jwe_header")
	public Environment evaluate(Environment env) {

		JsonObject jweHeader = env.getObject("credential_response_jwe_header");
		JsonElement zipEl = jweHeader.get("zip");

		// Check if the issuer metadata advertises zip_values_supported containing "DEF"
		boolean metadataAdvertisesDef = false;
		JsonElement zipValuesEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_response_encryption.zip_values_supported");
		if (zipValuesEl != null && zipValuesEl.isJsonArray()) {
			JsonArray zipValues = zipValuesEl.getAsJsonArray();
			for (JsonElement val : zipValues) {
				if (val.isJsonPrimitive() && "DEF".equals(OIDFJSON.getString(val))) {
					metadataAdvertisesDef = true;
					break;
				}
			}
		}

		if (zipEl != null) {
			String zip = OIDFJSON.getString(zipEl);
			logSuccess("Issuer applied compression to encrypted credential response",
				args("zip", zip, "jwe_header", jweHeader));
		} else if (metadataAdvertisesDef) {
			throw error("Issuer metadata advertises DEF in zip_values_supported but the encrypted "
					+ "credential response did not use compression despite the request including zip=DEF",
				args("jwe_header", jweHeader,
					"zip_values_supported", zipValuesEl));
		} else {
			logSuccess("Issuer did not apply compression (zip_values_supported does not advertise DEF)",
				args("jwe_header", jweHeader));
		}

		return env;
	}
}
