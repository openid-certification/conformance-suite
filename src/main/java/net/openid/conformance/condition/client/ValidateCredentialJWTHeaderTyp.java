package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateCredentialJWTHeaderTyp extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {

		String typ = env.getString("sdjwt", "credential.header.typ");

		// As per https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-11.html#section-3.2.1
		if (typ == null) {
			throw error("required header 'typ' is not present");
		}

		// As per https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-11.html#section-3.2.1
		if (!"dc+sd-jwt".equals(typ)) {
			throw error("required header 'typ' must be 'dc+sd-jwt'", args("typ", typ));
		}

		logSuccess("'typ' header is present", args("typ", typ));

		return env;
	}

}
