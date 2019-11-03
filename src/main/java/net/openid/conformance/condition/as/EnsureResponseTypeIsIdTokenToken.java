package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class EnsureResponseTypeIsIdTokenToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		String responseType = env.getString("authorization_endpoint_request", "params.response_type");

		if (Strings.isNullOrEmpty(responseType)) {
			throw error("Could not find response type in request");
		} else {
			String[] splitResponseTypeValue = responseType.split("( )+");
			Set<String> valuesSet = Set.of(splitResponseTypeValue);
			if(valuesSet.size() == 2 && valuesSet.contains("id_token") && valuesSet.contains("token")) {
				logSuccess("Response type is expected value", args("expected", "id_token token"));
				return env;
			}
			throw error("Response type is not expected value", args("expected", "id_token token", "actual", responseType));
		}
	}

}
