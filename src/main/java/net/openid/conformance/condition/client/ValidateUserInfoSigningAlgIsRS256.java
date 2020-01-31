package net.openid.conformance.condition.client;

import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ValidateUserInfoSigningAlgIsRS256 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "userinfo_object")
	public Environment evaluate(Environment env) {

		String userInfoObj = env.getString("userinfo_object", "value");

		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(userInfoObj);
			String alg = jwt.getHeader().getAlgorithm().getName();

			if (!alg.equals("RS256")) {
				throw error("userinfo response must be signed with RS256 as requested in the client registration", args("alg", alg));
			}

			logSuccess("userinfo response is signed with RS256", args("alg", alg));
			return env;

		} catch (ParseException e) {
			throw error("Error parsing userinfo response", e);
		}

	}

}
