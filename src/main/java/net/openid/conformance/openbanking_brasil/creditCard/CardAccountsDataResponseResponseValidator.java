package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger_credit_cards_apis.yaml
 * Api endpoint: /accounts
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Card List")
public class CardAccountsDataResponseResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ArrayField.Builder("data")
				.setMinItems(1)
				.build());
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		Set<String> enumProductType = Sets.newHashSet("CLASSIC_NACIONAL", "CLASSIC_INTERNACIONAL", "GOLD", "PLATINUM", "INFINITE, ELECTRON", "STANDARD_NACIONAL", "STANDARD_INTERNACIONAL", "ELETRONIC", "BLACK", "REDESHOP", "MAESTRO_MASTERCARD_MAESTRO", "GREEN", "BLUE", "BLUEBOX", "PROFISSIONAL_LIBERAL", "CHEQUE_ELETRONICO", "CORPORATIVO", "EMPRESARIAL", "COMPRAS", "BASICO_NACIONAL", "BASICO_INTERNACIONAL", "NANQUIM", "GRAFITE", "MAIS", "OUTROS");
		Set<String> enumCreditCardNetwork = Sets.newHashSet("VISA", "MASTERCARD", "AMERICAN_EXPRESS", "DINERS_CLUB", "HIPERCARD", "BANDEIRA_PROPRIA", "CHEQUE_ELETRONICO", "ELO", "OUTRAS");

		assertField(body,
			new StringField
				.Builder("creditCardAccountId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.build());

		assertField(body,
			new StringField
				.Builder("brandName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(80)
				.build());

		assertField(body,
			new StringField
				.Builder("companyCnpj")
				.setPattern("\\d{14}|^NA$")
				.setMaxLength(14)
				.build());

		assertField(body,
			new StringField
				.Builder("name")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.build());

		assertField(body,
			new StringField
				.Builder("productType")
				.setEnums(enumProductType)
				.setMaxLength(26)
				.build());

		assertField(body,
			new StringField
				.Builder("productAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("creditCardNetwork")
				.setMaxLength(17)
				.setEnums(enumCreditCardNetwork)
				.build());

		assertField(body,
			new StringField
				.Builder("networkAdditionalInfo")
				.setPattern("\\w*\\W*")
				.setMaxLength(50)
				.setOptional()
				.build());
	}
}
