package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class GenerateIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "user_info", "client" }, strings = "issuer")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		String subject = env.getString("user_info", "sub");
		String issuer = env.getString("issuer");
		String clientId = env.getString("client", "client_id");
		String nonce = env.getString("nonce");

		if (Strings.isNullOrEmpty(subject)) {
			throw error("Couldn't find subject");
		}

		if (Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find issuer");
		}

		if (Strings.isNullOrEmpty(clientId)) {
			throw error("Couldn't find client ID");
		}

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("sub", subject);
		claims.addProperty("aud", clientId);

		if (!Strings.isNullOrEmpty(nonce)) {
			claims.addProperty("nonce", nonce);
		}

		Instant iat = Instant.now();
		Instant exp = getExp(iat);

		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		env.putObject("id_token_claims", claims);

		logSuccess("Created ID Token Claims", claims);

		return env;

	}

	protected Instant getExp(Instant iat) {
		return iat.plusSeconds(5 * 60);
	}

}
