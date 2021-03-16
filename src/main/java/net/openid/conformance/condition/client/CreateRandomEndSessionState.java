package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

public class CreateRandomEndSessionState extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "end_session_state")
	public Environment evaluate(Environment env) {

		// https://openid.net/specs/openid-connect-session-1_0.html#RPLogout does not appear to define a character
		// set for state; assume it is in same as state defined in RFC6749
		String state = RFC6749AppendixASyntaxUtils.generateVSChar(50, 10, 30);

		// the way we encode urls escapes + incorrectly in the url query currently, so don't include '+'s
		// @see net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint_UnitTest.testEscape
		state = state.replace('+', '~');
		state = state.replace(' ', '~');

		// avoid ; as spring seems to not process them correctly when they're returned to us unescaped; see
		// https://gitlab.com/openid/conformance-suite/-/issues/871
		state = state.replace(';', '~');
		env.putString("end_session_state", state);

		log("Created end_session_state value", args("end_session_state", state));

		return env;
	}

}
