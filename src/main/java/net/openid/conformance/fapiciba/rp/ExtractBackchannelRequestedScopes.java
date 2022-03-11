package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;

public class ExtractBackchannelRequestedScopes extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveBackchannelRequestParameters.ENV_KEY)
	@PostEnvironment(strings = {"request_scopes_contain_openid"})
	public Environment evaluate(Environment env) {

		String scope = env.getString(CreateEffectiveBackchannelRequestParameters.ENV_KEY, CreateEffectiveBackchannelRequestParameters.SCOPE);

		if (Strings.isNullOrEmpty(scope)) {
			throw error("Missing scope parameter");
		} else {
			logSuccess("Requested scopes", args("scope", scope));
			String openidScopeRequested = "no";

			String[] scopes = scope.split(" ");
			for(String scopePiece : scopes) {
				if("openid".equals(scopePiece)) {
					openidScopeRequested = "yes";
					break;
				}
			}
			env.putString("request_scopes_contain_openid", openidScopeRequested);
			env.putString("scope", scope);

			return env;
		}

	}

}
