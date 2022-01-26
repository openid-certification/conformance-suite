package net.openid.conformance.openinsurance.discovery;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api Swagger URL: https://https://gitlab.com/obb1/certification/-/blob/master/src/main/resources/swagger/openinsurance/swagger-discovery.yaml
 * Api endpoint: /status/
 * Api version: 1.0.0
 * Api Git Hash: 17d932e0fac28570a0bf2a8b8e292a65b816f278
 */

@ApiName("Discovery Status")
public class StatusListValidator extends AbstractJsonAssertingCondition {
	public static final Set<String> CODE = Sets.newHashSet("OK","PARTIAL_FAILURE","UNAVAILABLE","SCHEDULED_OUTAGE");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectField
				.Builder("data")
				.setValidator(data ->
					assertField(data,
						new ObjectArrayField
							.Builder("status")
							.setValidator(this::assertStatus)
							.build()))
				.build());
		logFinalStatus();
		return environment;
	}

	private void assertStatus(JsonObject status) {
		assertField(status,
			new StringField
				.Builder("code")
				.setEnums(CODE)
				.build());

		assertField(status,
			new StringField
				.Builder("explanation")
				.build());

		assertField(status,
			new StringField
				.Builder("detectionTime")
				.setOptional()
				.build());

		assertField(status,
			new StringField
				.Builder("expectedResolutionTime")
				.setOptional()
				.build());

		assertField(status,
			new StringField
				.Builder("updateTime")
				.setOptional()
				.build());

		assertField(status,
			new StringArrayField
				.Builder("unavailableEndpoints")
				.setOptional()
				.build());
	}
}
