package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;

public class CreateClientAuthenticationAssertionClaimsWithIssAudience extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client", "server"})
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		String client_id = env.getString("client", "client_id");

		if (Strings.isNullOrEmpty(client_id)) {
			throw error("Couldn't find required configuration element", args("client_id", client_id));
		}

		JsonObject claims = new JsonObject();

		claims.addProperty("iss", client_id);
		claims.addProperty("sub", client_id);

		String audience = env.getString("server", "issuer");
		if (Strings.isNullOrEmpty(audience)) {
			throw error("Couldn't find required configuration element", args( "issuer", audience));
		}

		claims.addProperty("aud", audience);
		claims.addProperty("jti", RandomStringUtils.secure().nextAlphanumeric(20));

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(60);

		claims.addProperty("nbf", iat.getEpochSecond());
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		logSuccess("Created client assertion claims", claims);

		env.putObject("client_assertion_claims", claims);

		return env;

	}
}
