package net.openid.conformance.openbanking_brasil.common;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_common_apis.yaml
 * Api endpoint: GET /status
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 *
 */
@ApiName("Common Api GET Status")
public class GetStatusValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> CODE = Sets.newHashSet("OK", "PARTIAL_FAILURE", "UNAVAILABLE", "SCHEDULED_OUTAGE");

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
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
			new DatetimeField
				.Builder("detectionTime")
				.setOptional()
				.build());

		assertField(status,
			new DatetimeField
				.Builder("expectedResolutionTime")
				.setOptional()
				.build());

		assertField(status,
			new DatetimeField
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
