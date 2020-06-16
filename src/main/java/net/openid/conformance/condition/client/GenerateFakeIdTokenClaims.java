package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class GenerateFakeIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "server" })
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		String serverIssuerUrl = env.getString("server", "issuer");
		String clientId = env.getString("client", "client_id");

		if (Strings.isNullOrEmpty(serverIssuerUrl)) {
			throw error("Couldn't find issuer");
		}

		if (Strings.isNullOrEmpty(clientId)) {
			throw error("Couldn't find client ID");
		}

		JsonObject claims = new JsonObject();
		// aud/iss are deliberately the "wrong" way around here; this is an id_token "issued" by the client
		claims.addProperty("iss", clientId);
		claims.addProperty("sub", "SubjectID");
		claims.addProperty("aud", serverIssuerUrl);

		claims.addProperty("nonce", "flibble");

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);

		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		env.putObject("id_token_claims", claims);

		logSuccess("Created ID Token Claims", claims);

		return env;

	}

}
