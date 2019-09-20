package io.fintechlabs.testframework.condition.client;

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class SerializeRequestObjectWithNullAlgorithm extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		if (requestObjectClaims == null) {
			throw error("Couldn't find request object claims");
		}

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(requestObjectClaims.toString());

			PlainHeader header = new PlainHeader();

			PlainJWT requestObject = new PlainJWT(header, claimSet);

			env.putString("request_object", requestObject.serialize());

			logSuccess("Serialized the request object", args("request_object", requestObject.serialize()));

			return env;
		} catch (ParseException e) {
			throw error(e);
		}

	}

}
