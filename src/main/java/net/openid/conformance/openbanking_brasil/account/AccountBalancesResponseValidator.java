package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.StringField;

/**
 *  * API: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_accounts_apis.yaml
 *  * URL: /accounts/{accountId}/balances
 *  * Api git hash: f14f533cf29fdcef0a3ad38e2f49e1f31c5ab7b2
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
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
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
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
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
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		// Calls assert field too
		assertCurrencyType(data,
			new StringField
				.Builder("automaticallyInvestedAmountCurrency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

	}
}
