package net.openid.conformance.openbanking_brasil.creditCard.v1;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger_credit_cards_apis.yaml
 * Api endpoint: /accounts
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Card List")
public class CardAccountsDataResponseResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_PRODUCT_TYPE = Sets.newHashSet("CLASSIC_NACIONAL", "CLASSIC_INTERNACIONAL", "GOLD", "PLATINUM", "INFINITE", "ELECTRON", "STANDARD_NACIONAL", "STANDARD_INTERNACIONAL", "ELETRONIC", "BLACK", "REDESHOP", "MAESTRO_MASTERCARD_MAESTRO", "GREEN", "BLUE", "BLUEBOX", "PROFISSIONAL_LIBERAL", "CHEQUE_ELETRONICO", "CORPORATIVO", "EMPRESARIAL", "COMPRAS", "BASICO_NACIONAL", "BASICO_INTERNACIONAL", "NANQUIM", "GRAFITE", "MAIS", "OUTROS");
	public static final Set<String> ENUM_CREDIT_CARD_NETWORK = Sets.newHashSet("VISA", "MASTERCARD", "AMERICAN_EXPRESS", "DINERS_CLUB", "HIPERCARD", "BANDEIRA_PROPRIA", "CHEQUE_ELETRONICO", "ELO", "OUTRAS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectArrayField.Builder("data")
				.setValidator(this::assertInnerFields)
				.setMinItems(1)
				.build());

		return environment;
	}

	private void assertInnerFields(JsonObject body) {

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
				.setEnums(ENUM_PRODUCT_TYPE)
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
				.setEnums(ENUM_CREDIT_CARD_NETWORK)
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
