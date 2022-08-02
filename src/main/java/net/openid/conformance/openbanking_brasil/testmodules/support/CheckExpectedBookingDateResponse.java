package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckExpectedBookingDateResponse extends AbstractCheckExpectedDateResponse {

	@Override
	@PreEnvironment(
		required = {"resource_endpoint_response_full", "full_range_response"},
		strings = {"fromBookingDate", "toBookingDate"}
	)
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected String getToDate() {
		return "toBookingDate";
	}

	@Override
	protected String getFromDate() {
		return "fromBookingDate";
	}
}
