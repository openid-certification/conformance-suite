package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractFapiInteractionIdHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {

		String header = env.getString("incoming_request", "headers.x-fapi-interaction-id");
		if (Strings.isNullOrEmpty(header)) {
			env.removeNativeValue("fapi_interaction_id");
			throw error("Couldn't find FAPI interaction ID header");
		} else {

			env.putString("fapi_interaction_id", header);
			logSuccess("Found a FAPI interaction ID header", args("fapi_interaction_id", header));

			return env;

		}

	}

}
