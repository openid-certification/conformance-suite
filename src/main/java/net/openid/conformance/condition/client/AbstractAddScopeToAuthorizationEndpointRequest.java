package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractAddScopeToAuthorizationEndpointRequest extends AbstractCondition {

	protected Environment addScopeToAuthorizationEndpointRequest(Environment env, String scope) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		String oldScope = OIDFJSON.getString(authorizationEndpointRequest.get("scope"));
		String newScope;
		if (Strings.isNullOrEmpty(oldScope)) {
			newScope = scope;
		} else {
			newScope = oldScope + ' ' + scope;
		}
		authorizationEndpointRequest.remove("scope");
		authorizationEndpointRequest.addProperty("scope", newScope);
		logSuccess(String.format("Added \"%s\" to authorization endpoint request scope", scope),
				args("scope", newScope));
		return env;
	}
}
