package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.StringField;

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
		assertField(data,
			new DoubleField
				.Builder("overdraftContractedLimit")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		// Calls assert field too
		assertCurrencyType(data,
			new StringField
				.Builder("overdraftContractedLimitCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("overdraftUsedLimit")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		// Calls assert field too
		assertCurrencyType(data,
			new StringField
				.Builder("overdraftUsedLimitCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("unarrangedOverdraftAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("unarrangedOverdraftAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());
	}
}
