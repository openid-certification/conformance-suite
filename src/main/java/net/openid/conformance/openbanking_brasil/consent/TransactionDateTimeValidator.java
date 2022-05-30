package net.openid.conformance.openbanking_brasil.consent;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class TransactionDateTimeValidator  extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("resource_endpoint_response_status");

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		if (httpStatus != 400) {
			throw error("The endpoint returned a different http status than expected", args("actual", httpStatus, "expected", "400"));
		}

		logSuccess( "The endpoint http status code was " + httpStatus);

		return env;
	}
}
