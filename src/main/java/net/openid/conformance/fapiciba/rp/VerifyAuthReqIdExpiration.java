package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class VerifyAuthReqIdExpiration extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "auth_req_id_expiration")
	public Environment evaluate(Environment env) {

		if(isAuthReqIdExpired(env)) {
			JsonObject tokenEndpointResponse = new JsonObject();
			tokenEndpointResponse.addProperty("error", "expired_token");
			env.putObject("token_endpoint_response", tokenEndpointResponse);

			throw error("auth_req_id is expired", args("expired", env.getString("auth_req_id_expiration")));
		}

		logSuccess("auth_req_id is valid");
		return env;
	}

	public static boolean isAuthReqIdExpired(Environment env) {
		Instant authReqIdExpiration = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(env.getString("auth_req_id_expiration")));
		return Instant.now().isAfter(authReqIdExpiration);
	}
}
