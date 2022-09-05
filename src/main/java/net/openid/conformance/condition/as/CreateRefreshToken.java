package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

/**
 * https://tools.ietf.org/html/rfc6749#appendix-A.17
 *     The "refresh_token" element is defined in Sections 5.1 and 6:
 *
 *      refresh-token = 1*VSCHAR
 */
public class CreateRefreshToken extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "refresh_token")
	public Environment evaluate(Environment env) {

		String refreshToken = RFC6749AppendixASyntaxUtils.generateVSChar(50, 10, 5);

		env.putString("refresh_token", refreshToken);

		log("Created refresh token", args("refresh_token", refreshToken));

		return env;

	}

}
