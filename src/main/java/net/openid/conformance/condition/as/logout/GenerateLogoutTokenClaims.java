package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.UUID;

public class GenerateLogoutTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "user_info", "client", "session_state_data" }, strings = "issuer")
	@PostEnvironment(required = "logout_token_claims")
	public Environment evaluate(Environment env) {

		String subject = env.getString("user_info", "sub");
		String issuer = env.getString("issuer");
		String clientId = env.getString("client", "client_id");

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("sub", subject);
		claims.addProperty("aud", clientId);
		claims.addProperty("jti", UUID.randomUUID().toString());

		String sid = env.getString("session_state_data", "sid");
		if(sid!=null) {
			claims.addProperty("sid", sid);
		}

		JsonObject events = new JsonObject();
		events.add("http://schemas.openid.net/event/backchannel-logout", new JsonObject());
		claims.add("events", events);

		Instant iat = Instant.now();
		claims.addProperty("iat", iat.getEpochSecond());

		//OPs are encouraged to use short expiration times in Logout Tokens, preferably at most two minutes in the future
		Instant exp = iat.plusSeconds(2 * 60);
		claims.addProperty("exp", exp.getEpochSecond());

		claims.addProperty("ignored_claim", "Logout Tokens MAY contain other Claims. Any Claims used that are not understood MUST be ignored.");

		env.putObject("logout_token_claims", claims);

		logSuccess("Created Logout Token Claims", claims);

		return env;

	}

}
