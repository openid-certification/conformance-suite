package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CreateKSASignedConsentResponseClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "account_request_response", "server" })
	@PostEnvironment(required = "consent_response")
	public Environment evaluate(Environment env) {

		JsonObject message = env.getObject("account_request_response");
		String issuer = env.getString("server", "issuer");
		if (issuer == null || issuer.isEmpty()) {
			throw error("No server issuer available to set the consent response 'iss'");
		}

		Instant now = Instant.now();
		long iat = now.getEpochSecond();

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("iat", iat);
		claims.addProperty("nbf", iat);
		claims.addProperty("exp", now.plus(1, ChronoUnit.HOURS).getEpochSecond());
		claims.add("message", message);

		env.putObject("consent_response", claims);
		logSuccess("Created the KSA signed consent response claims", args("consent_response", claims));
		return env;
	}
}
