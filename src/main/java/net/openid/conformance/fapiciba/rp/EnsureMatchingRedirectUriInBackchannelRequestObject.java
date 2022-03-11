package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMatchingRedirectUriInBackchannelRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "backchannel_request_object" })
	public Environment evaluate(Environment env) {
		// get the client ID from the configuration
		String expected = env.getString("client", "redirect_uri");
		String actual = env.getString("backchannel_request_object", "claims.redirect_uri");

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Redirect URI matched",
				args("actual", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between redirect URI", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
