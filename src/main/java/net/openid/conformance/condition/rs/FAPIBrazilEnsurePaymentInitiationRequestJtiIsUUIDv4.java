package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilEnsurePaymentInitiationRequestJtiIsUUIDv4 extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"payment_initiation_request"})
	public Environment evaluate(Environment env) {
		String jti = env.getString("payment_initiation_request", "claims.jti");

		if (Strings.isNullOrEmpty(jti)) {
			throw error("Couldn't find jti in payment initiation request claims");
		}

		if(jti.matches("^([a-fA-F0-9]{8})-([a-fA-F0-9]{4})-4([a-fA-F0-9]{3})-[89ABab]([a-fA-F0-9]{3})-([a-fA-F0-9]{12})")) {
			logSuccess("jti claim in payment initiation request is a UUIDv4", args("jti", jti));
		} else {
			throw error("jti claim in payment initiation request is not a UUID v4",
				args("jti", jti));
		}



		return env;
	}

}
