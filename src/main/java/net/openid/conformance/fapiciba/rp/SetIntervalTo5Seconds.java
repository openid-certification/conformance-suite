package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetIntervalTo5Seconds extends AbstractCondition {

	public static final int DEFAULT_INTERVAL = 5;

	@Override
	public Environment evaluate(Environment env) {
		int interval = DEFAULT_INTERVAL;
		env.putInteger("interval", interval);

		logSuccess("Set interval", args("interval", interval));

		return env;
	}

}
