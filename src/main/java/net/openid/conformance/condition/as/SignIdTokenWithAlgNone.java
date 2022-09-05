package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SignIdTokenWithAlgNone extends AbstractCondition {

	private static final Base64URL ALG_NONE_HEADER = Base64URL.encode("{\"alg\":\"none\"}");

	@Override
	@PreEnvironment(required = { "id_token_claims"})
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("id_token_claims");
		String jwt =  ALG_NONE_HEADER + "." + Base64URL.encode(claims.toString()) + ".";
		logSuccess("Created id_token with alg none", args("id_token", jwt));
		env.putString("id_token", jwt);
		return env;
	}

}
