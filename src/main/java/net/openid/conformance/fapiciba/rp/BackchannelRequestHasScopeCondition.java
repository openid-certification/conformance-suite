package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class BackchannelRequestHasScopeCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		String scope = env.getString("backchannel_endpoint_http_request", "body_form_params.scope");
		if(scope == null) {
			throw error("The 'openid' scope parameter is required");
		}

		List<String> scopes = Splitter.on(" ").splitToList(scope);
		if(!scopes.contains("openid")) {
			throw error("The 'openid' scope parameter is required");
		}

		logSuccess("Backchannel authentication request contains scope 'openid'");

		return env;
	}

}
