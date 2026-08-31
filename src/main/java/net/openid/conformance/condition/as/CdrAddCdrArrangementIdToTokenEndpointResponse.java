package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class CdrAddCdrArrangementIdToTokenEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		// keep the same arrangement id for the lifetime of the test
		String cdrArrangementId = env.getString("cdr_arrangement_id");
		if (Strings.isNullOrEmpty(cdrArrangementId)) {
			cdrArrangementId = UUID.randomUUID().toString();
			env.putString("cdr_arrangement_id", cdrArrangementId);
		}

		JsonObject tokenEndpointResponse = env.getObject("token_endpoint_response");
		tokenEndpointResponse.addProperty("cdr_arrangement_id", cdrArrangementId);

		logSuccess("Added cdr_arrangement_id to token endpoint response", args("cdr_arrangement_id", cdrArrangementId));

		return env;
	}

}
