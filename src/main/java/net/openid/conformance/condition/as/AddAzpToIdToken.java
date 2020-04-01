package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class AddAzpToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"id_token_claims", "client"})
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");
		String clientId = env.getString("client", "client_id");
		claims.addProperty("azp", clientId);

		env.putObject("id_token_claims", claims);

		log("Added azp to ID token claims", args("id_token_claims", claims));

		return env;

	}

}
