package net.openid.conformance.openbanking_brasil.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.IntArrayField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_admin_apis.yaml
 * Api endpoint: GET /metrics
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 *
 */
@ApiName("Admin Api GET Metrics")
public class GetMetricsAdminApiValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder("data")
				.setValidator(this::assertData)
				.build());
		return environment;
	}

	protected void assertData(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("requestTime")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(100)
				.build());

		assertField(body,
			new ObjectField
				.Builder("availability")
				.setValidator(availability -> {

					assertField(availability,
						new ObjectField
							.Builder("uptime")
							.setValidator(uptime -> {
								assertField(uptime, new StringField.Builder("generalUptimeRate").build());
								assertField(uptime,
									new ObjectField
										.Builder("endpoints")
										.setValidator(this::assertUptimeEndpoints)
										.build());
							})
							.build());

					assertField(availability,
						new ObjectField
							.Builder("downtime")
							.setValidator(downtime -> {
								assertField(downtime, new IntField.Builder("generalDowntime").build());
								assertField(downtime, new IntField.Builder("scheduledOutage").build());
								assertField(downtime,
									new ObjectField
										.Builder("endpoints")
										.setValidator(this::assertDowntimeUptimeEndpoints)
										.build());
							})
							.build());
				})
				.build());

		assertField(body,
			new ObjectField
				.Builder("invocations")
				.setValidator(this::assertMetrics)
				.build());

		assertField(body,
			new ObjectField
				.Builder("averageResponse")
				.setValidator(this::assertMetrics)
				.build());

		assertField(body,
			new ObjectField
				.Builder("averageTps")
				.setValidator(this::assertDays)
				.build());

		assertField(body,
			new ObjectField
				.Builder("peakTps")
				.setValidator(this::assertDays)
				.build());

		assertField(body,
			new ObjectField
				.Builder("peakTps")
				.setValidator(this::assertDays)
				.build());

		assertField(body,
			new ObjectField
				.Builder("errors")
				.setValidator(this::assertDays)
				.build());

		assertField(body,
			new ObjectField
				.Builder("rejections")
				.setValidator(this::assertDays)
				.build());
	}

	private void assertUptimeEndpoints(JsonObject endpoints) {
		assertField(endpoints,
			new StringField
				.Builder("url")
				.build());

		assertField(endpoints,
			new StringField
				.Builder("uptimeRate")
				.build());
	}

	private void assertDowntimeUptimeEndpoints(JsonObject endpoints) {
		assertField(endpoints,
			new StringField
				.Builder("url")
				.build());

		assertField(endpoints,
			new IntField
				.Builder("partialDowntime")
				.build());
	}
	private void assertMetrics(JsonObject metrics) {
		assertField(metrics,
			new ObjectField
				.Builder("unauthenticated")
				.setValidator(this::assertDays)
				.build());

		assertField(metrics,
			new ObjectField
				.Builder("highPriority")
				.setValidator(this::assertDays)
				.build());

		assertField(metrics,
			new ObjectField
				.Builder("mediumPriority")
				.setValidator(this::assertDays)
				.build());

		assertField(metrics,
			new ObjectField
				.Builder("unattended")
				.setValidator(this::assertDays)
				.build());
	}

	private void assertDays(JsonObject data) {
		assertField(data, new IntField.Builder("currentDay").build());
		assertField(data, new IntArrayField.Builder("previousDays").build());
	}
}
