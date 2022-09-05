package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidStateToAuthorizationEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {
		String invalidState = env.getString(CreateAuthorizationEndpointResponseParams.ENV_KEY, CreateAuthorizationEndpointResponseParams.STATE);
		if(Strings.isNullOrEmpty(invalidState)) {
			invalidState = "1";
		} else {
			invalidState = invalidState.concat("1");
		}
		env.putString(CreateAuthorizationEndpointResponseParams.ENV_KEY, CreateAuthorizationEndpointResponseParams.STATE, invalidState);
		logSuccess("Added invalid state to authorization endpoint response params", args( "state", invalidState));
		return env;
	}

}
