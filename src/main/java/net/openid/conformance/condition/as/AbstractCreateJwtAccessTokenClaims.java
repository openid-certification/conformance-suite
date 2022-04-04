package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;

public abstract class AbstractCreateJwtAccessTokenClaims extends AbstractCondition {

	public Environment createJWTAccessTokenClaims(Environment env, String tokenName) {

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", env.getString("server", "issuer"));
		Instant iat = Instant.now();

		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("nbf", iat.getEpochSecond());
		claims.addProperty("exp", iat.plusSeconds(300).getEpochSecond());
		claims.addProperty("jti", RandomStringUtils.randomAlphanumeric(20));
		env.putObject(tokenName, claims);
		logSuccess("Created " + tokenName + " claims", claims);
		return env;
	}


}
