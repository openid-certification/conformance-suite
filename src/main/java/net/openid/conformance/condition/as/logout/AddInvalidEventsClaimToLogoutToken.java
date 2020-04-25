package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidEventsClaimToLogoutToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "logout_token_claims")
	@PostEnvironment(required = "logout_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("logout_token_claims");

		claims.remove("events");

		JsonObject events = new JsonObject();
		events.add("http://schemas.openid.net/event/foobar", new JsonObject());

		claims.add("events", events);

		env.putObject("logout_token_claims", claims);

		log("Added invalid events claim to logout token", args("logout_token_claims", claims));

		return env;

	}

}
