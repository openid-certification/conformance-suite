package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFInjectDummyStreamId extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String streamId = "dummy";
		env.putString("ssf","stream.stream_id_override", streamId);
		logSuccess("Injected dummy stream ID", args("stream_id", streamId));

		return env;
	}

	public static void undo(Environment env) {
		env.removeElement("ssf","stream.stream_id_override");
	}
}
