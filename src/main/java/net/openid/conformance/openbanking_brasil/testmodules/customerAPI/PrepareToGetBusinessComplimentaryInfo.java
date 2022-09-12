package net.openid.conformance.openbanking_brasil.testmodules.customerAPI;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareToGetBusinessComplimentaryInfo extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		setApi("customers");
		setEndpoint("/business/complimentary-information");

		return super.evaluate(env);
	}
}
