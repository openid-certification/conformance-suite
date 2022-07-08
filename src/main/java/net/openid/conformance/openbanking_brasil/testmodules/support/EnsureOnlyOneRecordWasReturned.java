package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

public class EnsureOnlyOneRecordWasReturned extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		assertField(bodyFrom(env),
			new ObjectArrayField
				.Builder("data")
				.setMinItems(1)
				.setMaxItems(1)
				.build());

		return env;
	}
}
