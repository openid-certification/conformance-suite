package net.openid.conformance.openbanking_brasil.common;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_common_apis.yaml
 * Api endpoint: GET /outages
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 *
 */
@ApiName("Common Api GET Outages")
public class GetOutagesValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertOutages)
				.build());

		return environment;
	}

	private void assertOutages(JsonObject outages) {
		assertField(outages,
			new DatetimeField
				.Builder("outageTime")
				.build());

		assertField(outages,
			new StringField
				.Builder("duration")
				.build());

		assertField(outages,
			new BooleanField
				.Builder("isPartial")
				.build());

		assertField(outages,
			new StringField
				.Builder("explanation")
				.build());
	}
}
