package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateCredentialJWTVct extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {

		String vct = env.getString("sdjwt", "credential.claims.vct");

		// As per https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-11.html#section-3.2.2.2-3.5.2.1
		if (vct == null) {
			throw error("required claim 'vct' is not present");
		}

		logSuccess("'vct' claim is present", args("vct", vct));

		return env;
	}

}
