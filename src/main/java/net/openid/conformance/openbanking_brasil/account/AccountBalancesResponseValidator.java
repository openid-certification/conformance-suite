package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.fields.DoubleField;
import net.openid.conformance.util.fields.StringField;

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

		assertDoubleField(data,
			new DoubleField
				.Builder("availableAmount")
				.build());

		assertStringField(data,
			new StringField
				.Builder("availableAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertDoubleField(data,
			new DoubleField
				.Builder("blockedAmount")
				.build());

		assertStringField(data,
			new StringField
				.Builder("blockedAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertDoubleField(data,
			new DoubleField
				.Builder("automaticallyInvestedAmount")
				.build());

		assertStringField(data,
			new StringField
				.Builder("automaticallyInvestedAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

	}
}
