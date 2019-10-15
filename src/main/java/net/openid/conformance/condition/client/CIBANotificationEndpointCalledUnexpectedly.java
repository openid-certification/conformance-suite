package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CIBANotificationEndpointCalledUnexpectedly extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Authorization server called CIBA notification endpoint in a case where it should not");
	}
}
