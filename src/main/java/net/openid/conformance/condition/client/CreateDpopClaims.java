package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;

public class CreateDpopClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = "dpop_proof_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = new JsonObject();

		claims.addProperty("jti", RandomStringUtils.secure().nextAlphanumeric(20));

		Instant iat = Instant.now();

		claims.addProperty("iat", iat.getEpochSecond());

		logSuccess("Created DPoP proof claims", claims);

		env.putObject("dpop_proof_claims", claims);

		return env;

	}
}
