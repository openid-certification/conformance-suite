package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.StringField;

/**
 * This is validator for API - Contas - Saldos da conta
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#saldos-da-conta">Saldos da conta</a>
 **/

@ApiName("Account Balances")
public class AccountBalancesResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);
		return environment;
	}

	private void assertInnerFields(JsonObject body) {

		JsonObject data = findByPath(body, "$.data").getAsJsonObject();

		assertField(data,
			new DoubleField
				.Builder("availableAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("availableAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("blockedAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("blockedAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("automaticallyInvestedAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("automaticallyInvestedAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

	}
}
