package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFInjectDummyStreamId extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.putString("ssf","stream.stream_id", "dummy");

		return env;
	}
}
