package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.fields.DoubleField;
import net.openid.conformance.util.fields.StringField;

/**
 * This validates Account Limits
 * https://openbanking-brasil.github.io/areadesenvolvedor/#limites-da-conta
 */
@ApiName("Account Limits")
public class AccountLimitsValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertInnerFields(body);
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		JsonObject data = findByPath(body, "$.data").getAsJsonObject();

		assertDoubleField(data,
			new DoubleField
				.Builder("overdraftContractedLimit")
				.build());

		assertStringField(data,
			new StringField
				.Builder("overdraftContractedLimitCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertDoubleField(data,
			new DoubleField
				.Builder("overdraftUsedLimit")
				.build());

		assertStringField(data,
			new StringField
				.Builder("overdraftUsedLimitCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertDoubleField(data,
			new DoubleField
				.Builder("unarrangedOverdraftAmount")
				.build());

		assertStringField(data,
			new StringField
				.Builder("unarrangedOverdraftAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());
	}
}
