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
 * <p>When {@link ValidateDiscoverySignedMetadata} decoded a {@code signed_metadata}
 * JWT into {@code authzen_signed_metadata_claims}, this condition overlays the
 * metadata claims onto the plain {@code pdp} object so all downstream checks see
 * the authoritative (signed) values. Registered JWT housekeeping claims
 * ({@code iss}, {@code iat}, {@code exp}, {@code nbf}, {@code aud}, {@code jti},
 * {@code sub}) are not metadata fields and are not copied. When no signed metadata
 * is present this is a no-op.
 */
public class ApplySignedMetadataPrecedence extends AbstractCondition {

	private static final Set<String> JWT_REGISTERED_CLAIMS = Set.of("iss", "iat", "exp", "nbf", "aud", "jti", "sub");

	@Override
	@PreEnvironment(required = "pdp")
	@PostEnvironment(required = "pdp")
	public Environment evaluate(Environment env) {
		if (!env.containsObject("authzen_signed_metadata_claims")) {
			logSuccess("No signed metadata present; plain discovery metadata is used as-is");
			return env;
		}
		JsonObject pdp = env.getObject("pdp");
		JsonObject claims = env.getObject("authzen_signed_metadata_claims");

		JsonObject applied = new JsonObject();
		for (Map.Entry<String, JsonElement> claim : claims.entrySet()) {
			if (JWT_REGISTERED_CLAIMS.contains(claim.getKey())) {
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
