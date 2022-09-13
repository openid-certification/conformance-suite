package net.openid.conformance.openinsurance.testmodule.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingPatrimonialRoot extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {
		setApi("insurance-patrimonial");
		setEndpoint("/");
		return env;
	}
}
