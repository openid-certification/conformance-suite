package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.variant.CIBAMode;

public class CheckCIBAModeIsPing extends AbstractCondition {
	@Override
	@PreEnvironment(strings = { "ciba_mode"})
	public Environment evaluate(Environment env) {
		CIBAMode cibaMode = Enum.valueOf(CIBAMode.class, env.getString("ciba_mode"));
		if (cibaMode != CIBAMode.PING) {
			throw error("Unexpected or disallowed mode", args("mode", cibaMode));
		}

		logSuccess("CIBA mode is correctly configured", args("mode", cibaMode));
		return env;
	}
}
