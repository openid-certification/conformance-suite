package net.openid.conformance.condition.as.jarm;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class GenerateJARMResponseClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", CreateAuthorizationEndpointResponseParams.ENV_KEY },
		strings = {"issuer", "authorization_code"})
	@PostEnvironment(required = "jarm_response_claims")
	public Environment evaluate(Environment env) {

		String issuer = env.getString("issuer");
		String code = env.getString("authorization_code");
		String clientId = env.getString("client", "client_id");
		String state = env.getString(CreateAuthorizationEndpointResponseParams.ENV_KEY, CreateAuthorizationEndpointResponseParams.STATE);

		//10 minutes
		Instant exp = Instant.now().plusSeconds(600);

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("aud", clientId);
		claims.addProperty("code", code);
		if(!Strings.isNullOrEmpty(state)) {
			claims.addProperty("state", state);
		}
		claims.addProperty("exp", exp.getEpochSecond());

		env.putObject("jarm_response_claims", claims);

		log("Created JARM response claims", claims);

		return env;

	}

}
