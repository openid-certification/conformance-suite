package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;

public class FAPIBrazilAddCPFAndCPNJToIdTokenClaims extends AbstractFAPIBrazilAddCPFAndCPNJToGeneratedClaims {

	@Override
	@PreEnvironment(required = { "id_token_claims", "authorization_request_object" })
	public Environment evaluate(Environment env) {
		if(addClaims(env, "id_token_claims", "id_token")) {
			logSuccess("Added claims to id_token claims", args("id_token_claims", env.getObject("id_token_claims")));
		}
		return env;
	}

}
