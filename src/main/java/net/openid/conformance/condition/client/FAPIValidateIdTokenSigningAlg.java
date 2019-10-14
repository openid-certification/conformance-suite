package net.openid.conformance.condition.client;

import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class FAPIValidateIdTokenSigningAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		String idToken = env.getString("id_token", "value");

		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(idToken);
			String alg = jwt.getHeader().getAlgorithm().getName();

			if (!alg.equals("PS256") && !alg.equals("ES256")) {
				throw error("id_token must be signed with PS256 or ES256", args("alg", alg));
			}

			logSuccess("id_token was signed with a permitted algorithm", args("alg", alg));
			return env;

		} catch (ParseException e) {
			throw error("Error parsing ID Token", e);
		}

	}

}
