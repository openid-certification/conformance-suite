package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestHasHintCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		String loginHintToken = env.getString("backchannel_endpoint_http_request", "body_form_params.login_hint_token");
		String idTokenHint = env.getString("backchannel_endpoint_http_request", "body_form_params.id_token_hint");
		String loginHint = env.getString("backchannel_endpoint_http_request", "body_form_params.login_hint");

		if(Strings.isNullOrEmpty(loginHintToken) && Strings.isNullOrEmpty(idTokenHint) && Strings.isNullOrEmpty(loginHint)) {
			throw error("One of 'login_hint_token', 'id_token_hint' or 'login_hint' must be present in the request");
		}

		logSuccess("Backchannel authentication request contains one of the required hint parameters");

		return env;
	}

}
