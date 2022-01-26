package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;
/**
 * Api: swagger_credit_cards_apis.yaml
 * Api endpoint: /accounts/{creditCardAccountId}
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Card Identification")
public class CardIdentificationResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_PRODUCT_TYPE = Sets.newHashSet("CLASSIC_NACIONAL", "CLASSIC_INTERNACIONAL", "GOLD", "PLATINUM", "INFINITE", "ELECTRON", "STANDARD_NACIONAL", "STANDARD_INTERNACIONAL", "ELETRONIC", "BLACK", "REDESHOP", "MAESTRO_MASTERCARD_MAESTRO", "GREEN", "BLUE", "BLUEBOX", "PROFISSIONAL_LIBERAL", "CHEQUE_ELETRONICO", "CORPORATIVO", "EMPRESARIAL", "COMPRAS", "BASICO_NACIONAL", "BASICO_INTERNACIONAL", "NANQUIM", "GRAFITE", "MAIS", "OUTROS");
	public static final Set<String> ENUM_CREDIT_CARD_NETWORK = Sets.newHashSet("VISA", "MASTERCARD", "AMERICAN_EXPRESS", "DINERS_CLUB", "HIPERCARD", "BANDEIRA_PROPRIA", "CHEQUE_ELETRONICO", "ELO", "OUTRAS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);

		return environment;
	}

	private void assertInnerFields(JsonElement body) {
		JsonObject data = findByPath(body, "$.data").getAsJsonObject();

		assertField(data,
			new StringField
				.Builder("name")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.build());

		assertField(data,
			new StringField
				.Builder("productType")
				.setEnums(ENUM_PRODUCT_TYPE)
				.setMaxLength(26)
				.build());

		assertField(data,
			new StringField
				.Builder("productAdditionalInfo")
				.setOptional()
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.build());

		assertField(data,
			new StringField
				.Builder("creditCardNetwork")
				.setEnums(ENUM_CREDIT_CARD_NETWORK)
				.setMaxLength(17)
				.build());

		assertField(data,
			new StringField
				.Builder("networkAdditionalInfo")
				.setOptional()
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.build());

		assertField(data,
			new ObjectArrayField.
				Builder("paymentMethod")
				.setValidator(this::assertInnerFieldsPaymentMethod)
				.setMinItems(1)
				.build());
	}


	private void assertInnerFieldsPaymentMethod(JsonObject jsonObject) {
		assertField(jsonObject,
			new StringField
				.Builder("identificationNumber")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.build());

		assertField(jsonObject,
			new BooleanField
				.Builder("isMultipleCreditCard")
				.build());
	}
}
