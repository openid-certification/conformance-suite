package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class SignIdTokenWithNullAlgorithm extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims" )
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {

		JsonObject idTokenClaims = env.getObject("id_token_claims");

		try {

			JWTClaimsSet claimSet = JWTClaimsSet.parse(idTokenClaims.toString());

			PlainHeader header = new PlainHeader();

			PlainJWT idToken  = new PlainJWT(header, claimSet);

			env.putString("id_token", idToken.serialize());

			logSuccess("Signed the id_token with null algorithm", args("id_token", idToken, "id_token serialized", idToken.serialize()));

			return env;
		} catch (ParseException e) {
			throw error(e);
		}

	}

}
