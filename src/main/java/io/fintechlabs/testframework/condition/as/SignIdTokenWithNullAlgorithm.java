package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.text.ParseException;

public class SignIdTokenWithNullAlgorithm extends AbstractCondition {

	public SignIdTokenWithNullAlgorithm(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

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
