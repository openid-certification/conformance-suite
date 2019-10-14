package net.openid.conformance.condition.client;

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
