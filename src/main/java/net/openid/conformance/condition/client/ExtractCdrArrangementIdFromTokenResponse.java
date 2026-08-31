package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractCdrArrangementIdFromTokenResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(strings = "cdr_arrangement_id")
	public Environment evaluate(Environment env) {

		String cdrArrangementId = env.getString("token_endpoint_response", "cdr_arrangement_id");

		if (Strings.isNullOrEmpty(cdrArrangementId)) {
			throw error("cdr_arrangement_id is missing from the token endpoint response; CDR requires it to be returned when a consent is established",
				args("token_endpoint_response", env.getObject("token_endpoint_response")));
		}

		env.putString("cdr_arrangement_id", cdrArrangementId);

		logSuccess("Extracted cdr_arrangement_id from token endpoint response", args("cdr_arrangement_id", cdrArrangementId));

		return env;
	}

}
