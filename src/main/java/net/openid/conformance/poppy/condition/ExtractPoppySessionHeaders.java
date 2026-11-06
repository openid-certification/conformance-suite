package net.openid.conformance.poppy.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractPoppySessionHeaders extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = {"poppy_session_jwt", "poppy_version"})
	public Environment evaluate(Environment env) {
		String session = env.getString("incoming_request", "headers.poppy-session");
		String version = env.getString("incoming_request", "headers.poppy-version");

		if (session == null || session.isEmpty()) {
			throw error("The request has no Poppy-Session header. An endpoint whose handling depends on Session claims must reject such a request with invalid_session (section 5.3).",
				args("headers", env.getElementFromObject("incoming_request", "headers")));
		}
		if (version == null || version.isEmpty()) {
			throw error("The request has no Poppy-Version header. An endpoint that verifies the Session header must reject such a request with version_unsupported (section 3.4).",
				args("headers", env.getElementFromObject("incoming_request", "headers")));
		}

		env.putString("poppy_session_jwt", session);
		env.putString("poppy_version", version);
		logSuccess("The request carries Poppy-Session and Poppy-Version headers", args("poppy_session", session, "poppy_version", version));
		return env;
	}
}
