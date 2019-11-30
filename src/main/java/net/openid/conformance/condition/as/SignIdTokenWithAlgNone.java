package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.codec.binary.Base64;

import java.text.ParseException;

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
