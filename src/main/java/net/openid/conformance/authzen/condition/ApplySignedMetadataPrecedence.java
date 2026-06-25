package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Apply the precedence rule for signed PDP discovery metadata (certification
 * profile https://github.com/openid/authzen/issues/433 §6.4): "Metadata values in
 * the signed token take precedence over plain JSON values in the response."
 *
 * <p>When {@link ExtractPDPSignedMetadata} decoded a {@code signed_metadata} JWT
 * into {@code pdp_signed_metadata} (with the decoded claims under its
 * {@code claims} member), this condition overlays the metadata claims onto the
 * plain {@code pdp} object so all downstream checks see the authoritative (signed)
 * values. Registered JWT housekeeping claims ({@code iss}, {@code iat},
 * {@code exp}, {@code nbf}, {@code aud}, {@code jti}, {@code sub}) are not metadata
 * fields and are not copied. This condition only runs when {@code signed_metadata}
 * is present (see {@link net.openid.conformance.authzen.AbstractAuthzenPDPTest}).
 */
public class ApplySignedMetadataPrecedence extends AbstractCondition {

	private static final Set<String> JWT_IGNORED_CLAIMS = Set.of("iss", "iat", "exp", "nbf", "aud", "jti", "sub", "signed_metadata");

	@Override
	@PreEnvironment(required = {"pdp", "pdp_signed_metadata"})
	@PostEnvironment(required = "pdp")
	public Environment evaluate(Environment env) {
		JsonObject pdp = env.getObject("pdp");
		JsonElement claimsElement = env.getElementFromObject("pdp_signed_metadata", "claims");
		if(claimsElement==null) {
			throw error("No claims in pdp_signed_metadata");
		}
		JsonObject claims = claimsElement.getAsJsonObject();

		JsonObject applied = new JsonObject();
		for (Map.Entry<String, JsonElement> claim : claims.entrySet()) {
			if (JWT_IGNORED_CLAIMS.contains(claim.getKey())) {
				continue;
			}
			pdp.add(claim.getKey(), claim.getValue());
			applied.add(claim.getKey(), claim.getValue());
		}

		env.putObject("pdp", pdp);
		logSuccess("Applied signed metadata values over the plain discovery metadata (signed values take precedence)",
			args("applied", applied));
		return env;
	}
}
