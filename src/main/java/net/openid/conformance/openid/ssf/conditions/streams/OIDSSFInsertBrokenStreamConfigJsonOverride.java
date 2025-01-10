package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFInsertBrokenStreamConfigJsonOverride extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String value = ";{ broken";
		env.putString("ssf", "stream.config_override_json", value);

		logSuccess("OIDSSF insert broken stream", args("config", value));

		return env;
	}

	public static void undo(Environment env) {
		env.removeElement("ssf", "stream.config_override_json");
	}
}
