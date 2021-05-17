package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class GetConsentResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data.brandID");
		assertHasStringField(body, "$.data.brandName");
		assertHasStringField(body, "$.data.consentId");
		assertHasStringField(body, "$.data.creationDateTime");
		assertHasStringField(body, "$.data.status");
		assertHasStringField(body, "$.data.statusUpdateDateTime");
		assertHasStringField(body, "$.data.expirationDateTime");
		assertHasStringField(body, "$.data.transactionFromDateTime");
		assertHasStringField(body, "$.data.transactionToDateTime");
		assertHasStringArrayField(body, "$.data.permissions");

		return environment;
	}

}
