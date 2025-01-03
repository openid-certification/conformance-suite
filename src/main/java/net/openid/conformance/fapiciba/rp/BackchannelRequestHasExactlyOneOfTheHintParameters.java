package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.stream.Stream;

public class BackchannelRequestHasExactlyOneOfTheHintParameters extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {

		String loginHintToken = env.getString("backchannel_request_object", "claims.login_hint_token");
		String idTokenHint = env.getString("backchannel_request_object", "claims.id_token_hint");
		String loginHint = env.getString("backchannel_request_object", "claims.login_hint");

		if(Stream.of(loginHintToken, idTokenHint, loginHint).filter(s -> !Strings.isNullOrEmpty(s)).count() != 1) {
			throw error("Exactly one of 'login_hint_token', 'id_token_hint' or 'login_hint' must be present in the request",
				args("login_hint", loginHint, "login_hint_token", loginHintToken, "id_token_hint", idTokenHint));
		}

		logSuccess("Backchannel authentication request contains one of the required hint parameters");

		return env;
	}

}
