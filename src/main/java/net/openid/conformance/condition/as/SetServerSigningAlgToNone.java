package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class SetServerSigningAlgToNone extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "signing_algorithm")
	public Environment evaluate(Environment env) {
		env.putString("signing_algorithm", "none");
		log("Successfully set signing algorithm to none", args("signing_algorithm", "none"));
		return env;
	}

}
