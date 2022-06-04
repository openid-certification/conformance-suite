package net.openid.conformance.openbanking_brasil.testmodules.customerAPI;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareToGetBusinessIdentifications extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		setApi("customers");
		setEndpoint("/business/identifications");

		return super.evaluate(env);
	}
}
