package net.openid.conformance.openbanking_brasil.account.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAdditionalAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: swagger/openBanking/swagger_accounts_apis-v2.yaml
 * Api endpoint: /accounts/{accountId}/balances
 * Api version: 2.0.0.final
 **/
@ApiName("Account Balances V2")
public class AccountBalancesResponseValidatorV2 extends AbstractJsonAdditionalAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder("data")
				.setValidator(this::assertData)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonElement data) {
		assertField(data,
			new ObjectField
				.Builder("availableAmount")
				.setValidator(this::assertAvailableAmount)
				.build());

		assertField(data,
			new ObjectField
				.Builder("blockedAmount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new ObjectField
				.Builder("automaticallyInvestedAmount")
				.setValidator(this::assertAmount)
				.build());
	}

	private void assertAvailableAmount(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("amount")
				.setMinLength(4)
				.setMaxLength(21)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setMaxLength(3)
				.build());
	}

	private void assertAmount(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("amount")
				.setMinLength(4)
				.setMaxLength(20)
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setMaxLength(3)
				.build());
	}
}
