package net.openid.conformance.openid.federation;

import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ValidateJwksAreEqual extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "server_jwks" } )
	public Environment evaluate(Environment env) {

		try {
			JWKSet preconfiguredKeys = JWKSet.parse(env.getElementFromObject("config", "federation.trust_anchor_jwks").toString());
			JWKSet actualKeys = JWKSet.parse(env.getObject("server_jwks").toString());
			if (!preconfiguredKeys.equals(actualKeys)) {
				throw error("The keys in trust anchor's entity configuration do not match pre-configured trust anchor keys",
					args("preconfigured", preconfiguredKeys, "actual", actualKeys));
			}
		} catch (ParseException e) {
			throw error("Failed to parse JWK set", e);
		}

		logSuccess("The keys in trust anchor's entity configuration match pre-configured trust anchor keys");
		return env;
	}
}
