package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilEnsureConsentRequestJtiIsUUIDv4 extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"new_consent_request"})
	public Environment evaluate(Environment env) {
		String jti = env.getString("new_consent_request", "claims.jti");

		if (Strings.isNullOrEmpty(jti)) {
			throw error("Couldn't find jti in consent request claims");
		}

		if(jti.matches("^([a-fA-F0-9]{8})-([a-fA-F0-9]{4})-4([a-fA-F0-9]{3})-[89ABab]([a-fA-F0-9]{3})-([a-fA-F0-9]{12})")) {
			logSuccess("jti claim in consent request is a UUIDv4", args("jti", jti));
		} else {
			throw error("jti claim in consent request is not a UUID v4",
				args("jti", jti));
		}



		return env;
	}

}
