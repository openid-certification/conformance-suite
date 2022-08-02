package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckExpectedTransactionDateMaxLimitedResponse extends AbstractCheckExpectedDateResponse{

	@Override
	@PreEnvironment(
		required = {"resource_endpoint_response_full", "full_range_response"},
		strings = {"fromTransactionDateMaxLimited", "toTransactionDateMaxLimited"}
	)
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected String getToDateName() {
		return "toTransactionDateMaxLimited";
	}

	@Override
	protected String getFromDateName() {
		return "fromTransactionDateMaxLimited";
	}
}
