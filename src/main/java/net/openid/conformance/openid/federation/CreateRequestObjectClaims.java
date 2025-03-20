package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.UUID;

public class CreateRequestObjectClaims extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		String entityIdentifier = env.getString("base_url");
		String scope = "openid";
		String responseType = "code";
		String redirectUri = entityIdentifier + "/callback";
		String aud = env.getString("config", "federation.entity_identifier");
		String jti = UUID.randomUUID().toString();
		long exp = Instant.now().plusSeconds(5 * 60).getEpochSecond();

		JsonObject requestObjectClaims = new JsonObject();
		requestObjectClaims.addProperty("client_id", entityIdentifier);
		requestObjectClaims.addProperty("scope", scope);
		requestObjectClaims.addProperty("response_type", responseType);
		requestObjectClaims.addProperty("redirect_uri", redirectUri);
		requestObjectClaims.addProperty("iss", entityIdentifier);
		requestObjectClaims.addProperty("aud", aud);
		requestObjectClaims.addProperty("jti", jti);
		requestObjectClaims.addProperty("exp", exp);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Created request object claims", args("request_object_claims", requestObjectClaims));

		return env;
	}

}
