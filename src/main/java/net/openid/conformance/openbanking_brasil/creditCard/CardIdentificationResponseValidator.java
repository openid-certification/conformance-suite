package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This is validator for API - Cartão de Crédito "Identificação de cartão de crédito"
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#saldos-da-conta
 **/
@ApiName("Card Identification")
public class CardIdentificationResponseValidator extends AbstractJsonAssertingCondition {

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
		Set<String> enumProductType = Sets.newHashSet("CLASSIC_NACIONAL", "CLASSIC_INTERNACIONAL", "GOLD", "PLATINUM", "INFINITE", "ELECTRON", "STANDARD_NACIONAL", "STANDARD_INTERNACIONAL", "ELETRONIC", "BLACK", "REDESHOP", "MAESTRO_MASTERCARD_MAESTRO", "GREEN", "BLUE", "BLUEBOX", "PROFISSIONAL_LIBERAL", "CHEQUE_ELETRONICO", "CORPORATIVO", "EMPRESARIAL", "COMPRAS", "BASICO_NACIONAL", "BASICO_INTERNACIONAL", "NANQUIM", "GRAFITE", "MAIS", "OUTROS");
		Set<String> enumCreditCardNetwork = Sets.newHashSet("VISA", "MASTERCARD", "AMERICAN_EXPRESS", "DINERS_CLUB", "HIPERCARD", "BANDEIRA_PROPRIA", "CHEQUE_ELETRONICO", "ELO", "OUTRAS");

		assertField(data,
			new StringField
				.Builder("name")
				//.setPattern("\\w*\\W*") //TODO wrong pattern
				.setMaxLength(50)
				.build());

		assertField(data,
			new StringField
				.Builder("productType")
				.setEnums(enumProductType)
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
				.setEnums(enumCreditCardNetwork)
				.setMaxLength(17)
				.build());

		assertField(data,
			new StringField
				.Builder("networkAdditionalInfo")
				.setOptional()
				.setPattern("\\w*\\W*")
				.setMaxLength(50)
				.build());

		assertField(data,
			new ArrayField.
				Builder("paymentMethod")
				.setMinItems(1)
				.build());


		assertJsonArrays(data, "paymentMethod", this::assertInnerFieldsPaymentMethod);
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
