package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetIntervalToPlus5Seconds extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		int interval = SetIntervalTo5Seconds.DEFAULT_INTERVAL + env.getInteger("interval");
		env.putInteger("interval", interval);

		logSuccess("Set interval", args("interval", interval));

		return env;
	}

}
