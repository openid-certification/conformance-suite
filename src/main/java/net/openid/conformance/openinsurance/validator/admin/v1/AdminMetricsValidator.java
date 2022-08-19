package net.openid.conformance.openinsurance.validator.admin.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

/**
 * Api source: swagger/openinsurance/admin/v1/swagger-admin-metrics.yaml
 * Api endpoint: /metrics
 * Api git hash: a0cf93fb358df175adea537178f1980078014836
 * Api version: 1.2.0
 */
@ApiName("Admin Metrics")
public class AdminMetricsValidator extends AbstractJsonAssertingCondition {

	private final OpenInsuranceLinksAndMetaValidator linksAndMetaValidator = new OpenInsuranceLinksAndMetaValidator(this);

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body, new ObjectField.Builder("data").setValidator(data -> {

			assertField(data, new StringField.Builder("requestTime").setPattern("^((?:(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?))(Z|[\\+-]\\d{2}:\\d{2})?)$").build());

			assertField(data, new ObjectField.Builder("availability")
				.setValidator(this::assertAvailabilityMetrics).build());

			assertField(data, new ObjectField.Builder("invocations")
				.setValidator(this::assertInvocations).build());

			assertField(data, new ObjectField.Builder("averageResponse")
				.setValidator(this::assertInvocations).build());

			assertField(data, new ObjectField.Builder("averageTps")
				.setValidator(this::assertInvocation).build());

			assertField(data, new ObjectField.Builder("peakTps")
				.setValidator(this::assertInvocation).build());

			assertField(data, new ObjectField.Builder("errors")
				.setValidator(this::assertInvocation).build());

			assertField(data, new ObjectField.Builder("rejections")
				.setValidator(this::assertInvocation).build());

		}).build());

		assertField(body,
				new ObjectField
						.Builder("links")
						.setValidator(linksAndMetaValidator::assertLinks)
						.build());

		assertField(body,
				new ObjectField
						.Builder("meta")
						.setValidator(linksAndMetaValidator::assertMeta)
						.setOptional()
						.build());

		logFinalStatus();
		return environment;
	}

	private void assertAvailabilityMetrics(JsonObject availability) {
		assertField(availability, new ObjectField.Builder("uptime").setValidator(uptime -> {

			assertField(uptime, new StringField.Builder("generalUptimeRate").setPattern("^\\d{1}\\.\\d{1,16}$").build());

			assertField(uptime, new ObjectArrayField.Builder("endpoints").setValidator(endpoint -> {

				assertField(endpoint, new StringField.Builder("url").build());

				assertField(endpoint, new StringField.Builder("uptimeRate").setPattern("^\\d{1}\\.\\d{1,16}$").build());

			}).build());
		}).build());

		assertField(availability, new ObjectField.Builder("downtime").setValidator(downtime -> {

			assertField(downtime, new IntField.Builder("generalDowntime").build());

			assertField(downtime, new IntField.Builder("scheduledOutage").build());

			assertField(downtime, new ObjectArrayField.Builder("endpoints").setValidator(endpoint -> {

				assertField(endpoint, new StringField.Builder("url").build());

				assertField(endpoint, new IntField.Builder("partialDowntime").build());

			}).build());
		}).build());
	}

	private void assertInvocations(JsonObject invocations) {
		assertField(invocations, new ObjectField.Builder("unauthenticated")
			.setValidator(this::assertInvocation).build());
		assertField(invocations, new ObjectField.Builder("highPriority")
			.setValidator(this::assertInvocation).build());
		assertField(invocations, new ObjectField.Builder("mediumPriority")
			.setValidator(this::assertInvocation).build());
		assertField(invocations, new ObjectField.Builder("unattended")
			.setValidator(this::assertInvocation).build());
	}

	private void assertInvocation(JsonObject invocation) {
		assertField(invocation, new IntField.Builder("currentDay").build());
		assertField(invocation, new IntArrayField.Builder("previousDays").build());
	}
}



