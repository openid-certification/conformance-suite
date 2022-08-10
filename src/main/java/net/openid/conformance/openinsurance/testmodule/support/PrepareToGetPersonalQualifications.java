package net.openid.conformance.openinsurance.testmodule.support;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;


public class PrepareToGetPersonalQualifications extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		setApi("customers");
		setEndpoint("/personal/qualifications");

		return super.evaluate(env);
	}
}
