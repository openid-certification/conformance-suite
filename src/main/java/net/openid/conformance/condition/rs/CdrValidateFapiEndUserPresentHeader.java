package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrValidateFapiEndUserPresentHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String header = env.getString("incoming_request", "headers.x-fapi-end-user-present");

		if (Strings.isNullOrEmpty(header)) {
			throw error("The x-fapi-end-user-present header must be sent on resource endpoint requests");
		}

		if (!"true".equals(header) && !"false".equals(header)) {
			throw error("The x-fapi-end-user-present header must be 'true' or 'false'", args("x-fapi-end-user-present", header));
		}

		logSuccess("Resource endpoint request contains a valid x-fapi-end-user-present header", args("x-fapi-end-user-present", header));

		return env;
	}

}
