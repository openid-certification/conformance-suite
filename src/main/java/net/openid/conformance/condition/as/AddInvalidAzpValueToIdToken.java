package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class AddInvalidAzpValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		//use a random value
		String azpValue = "random_azp_value_" + RandomStringUtils.randomAlphanumeric(10);
		claims.addProperty("azp", azpValue);

		env.putObject("id_token_claims", claims);

		log("Added invalid azp to ID token claims", args("id_token_claims", claims, "azp", azpValue));

		return env;

	}

}
