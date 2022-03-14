package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Splitter;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class BackchannelRequestHasScopeCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	@PostEnvironment(strings = "scope")
	public Environment evaluate(Environment env) {

		String scope = env.getString("backchannel_request_object", "claims.scope");
		if(scope == null) {
			throw error("The 'openid' scope parameter is required");
		}

		List<String> scopes = Splitter.on(" ").splitToList(scope);
		if(!scopes.contains("openid")) {
			throw error("The 'openid' scope parameter is required");
		}

		env.putString("scope", scope);
		logSuccess("Backchannel authentication request contains scope 'openid'");

		return env;
	}

}
