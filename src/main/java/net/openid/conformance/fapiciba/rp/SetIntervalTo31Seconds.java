package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetIntervalTo31Seconds extends AbstractCondition {

	@Override
	@PostEnvironment(strings = { "interval" })
	public Environment evaluate(Environment env) {
		int interval = 31;
		env.putInteger("interval", interval);

		logSuccess("Set interval", args("interval", interval));

		return env;
	}

}
