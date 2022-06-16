package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

public class EnsureAccountListIsEmpty extends AbstractJsonAssertingCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectArrayField.Builder("data")
				.setMinItems(0)
				.setMaxItems(0)
				.build());

		return environment;
	}
}
