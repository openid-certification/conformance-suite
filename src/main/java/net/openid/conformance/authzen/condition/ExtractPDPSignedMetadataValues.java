package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

/**
 * Collects the metadata values a verified signed_metadata JWT carries (Section 9.1.3), without the
 * JWT's own claims, into {@code pdp_signed_metadata_values}, so they can be checked like a
 * published metadata document before {@link ApplySignedMetadataPrecedence} puts them in effect.
 */
public class ExtractPDPSignedMetadataValues extends AbstractCondition {

	@Override
	@PreEnvironment(required = "pdp_signed_metadata")
	@PostEnvironment(required = "pdp_signed_metadata_values")
	public Environment evaluate(Environment env) {
		JsonElement claimsElement = env.getElementFromObject("pdp_signed_metadata", "claims");
		if (claimsElement == null || !claimsElement.isJsonObject()) {
			throw error("No claims in the signed_metadata JWT", args("pdp_signed_metadata", env.getObject("pdp_signed_metadata")));
		}
		JsonObject values = new JsonObject();
		for (Map.Entry<String, JsonElement> claim : claimsElement.getAsJsonObject().entrySet()) {
			if (!ApplySignedMetadataPrecedence.JWT_CLAIMS.contains(claim.getKey())) {
				values.add(claim.getKey(), claim.getValue().deepCopy());
			}
		}
		env.putObject("pdp_signed_metadata_values", values);
		logSuccess("Collected the metadata values carried in signed_metadata", args("values", values));
		return env;
	}
}
