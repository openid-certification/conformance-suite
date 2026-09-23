package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateTrustAnchorSubordinateStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "federation_response_jwt", "trust_anchor" }, strings = "trust_anchor_entity_identifier")
	@PostEnvironment(required = "trust_anchor_fetch_response_claims")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getElementFromObject("federation_response_jwt", "claims").getAsJsonObject();
		claims.remove("authority_hints");
		claims.remove("trust_mark_issuers");
		claims.remove("trust_mark_owners");
		claims.addProperty("iss", env.getString("trust_anchor_entity_identifier"));
		claims.addProperty("source_endpoint", env.getString("trust_anchor", "metadata.federation_entity.federation_fetch_endpoint"));
		env.putObject("trust_anchor_fetch_response_claims", claims);

		logSuccess("Created trust anchor subordinate statement", args("claims", claims));
		return env;
	}

}
