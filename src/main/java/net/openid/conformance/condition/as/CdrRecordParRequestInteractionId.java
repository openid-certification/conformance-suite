package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrRecordParRequestInteractionId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String header = env.getString("incoming_request", "headers.x-fapi-interaction-id");

		if (Strings.isNullOrEmpty(header)) {
			log("No x-fapi-interaction-id header in the PAR request");
			return env;
		}

		env.putString("par_request_fapi_interaction_id", header);
		logSuccess("Recorded the x-fapi-interaction-id from the PAR request", args("par_request_fapi_interaction_id", header));

		return env;
	}

}
