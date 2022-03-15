package net.openid.conformance.openinsurance.validator.discovery;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

/**
 * Api Swagger URL: https://https://gitlab.com/obb1/certification/-/blob/master/src/main/resources/swagger/openinsurance/swagger-discovery.yaml
 * Api endpoint: /outages/
 * Api version: 1.0.0
 * Api Git Hash: 17d932e0fac28570a0bf2a8b8e292a65b816f278
 */

@ApiName("Discovery Outages")
public class OutagesListValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.build());

		new OpenInsuranceLinksAndMetaValidator(this).assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("outageTime")
				.build());

		assertField(data,
			new StringField
				.Builder("duration")
				.build());

		assertField(data,
			new BooleanField
				.Builder("isPartial")
				.build());

		assertField(data,
			new StringField
				.Builder("explanation")
				.build());

		assertField(data,
			new StringArrayField
				.Builder("unavailableEndpoints")
				.build());
	}
}
