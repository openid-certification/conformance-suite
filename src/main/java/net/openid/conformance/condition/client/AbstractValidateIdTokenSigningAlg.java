package net.openid.conformance.condition.client;

import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.Set;

public abstract class AbstractValidateIdTokenSigningAlg extends AbstractCondition {

	abstract Set<String> getPermitted();

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		final Set<String> permitted = getPermitted();

		String idToken = env.getString("id_token", "value");

		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(idToken);
			String alg = jwt.getHeader().getAlgorithm().getName();

			if (!permitted.contains(alg)) {
				throw error("id_token must be signed with a permitted alg",
					args("alg", alg, "permitted", permitted));
			}

			logSuccess("id_token was signed with a permitted algorithm",
				args("alg", alg, "permitted", permitted));
			return env;

		} catch (ParseException e) {
			throw error("Error parsing ID Token", e);
		}

	}
}
