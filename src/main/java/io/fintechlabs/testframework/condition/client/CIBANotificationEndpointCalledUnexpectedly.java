package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class CIBANotificationEndpointCalledUnexpectedly extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Authorization server called CIBA notification endpoint in a case where it should not");
	}
}
