package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

public class EnsureAtLeastSpecifiedNumberOfRecordsWereReturned extends AbstractJsonAssertingCondition {


	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer requiredNumberOfRecords = env.getInteger("required_number_of_records");
		if (requiredNumberOfRecords != null) {
			JsonElement body = bodyFrom(env);
			assertField(body,
				new ObjectArrayField
					.Builder("data")
					.setMinItems(requiredNumberOfRecords)
					.build());
		} else {
			throw error("required_number_of_records was not found in the environment. This is bug");
		}
		return env;
	}
}
