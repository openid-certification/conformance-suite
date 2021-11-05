package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReverseScopeOrderInAuthorizationEndpointRequest extends AbstractReverseScopeOrder {
	public static final String envKey = "authorization_endpoint_request";

	@Override
	@PreEnvironment(required = envKey)
	@PostEnvironment(required = envKey)
	public Environment evaluate(Environment env) {

		reverseScope(env, envKey);

		return env;

	}

}
