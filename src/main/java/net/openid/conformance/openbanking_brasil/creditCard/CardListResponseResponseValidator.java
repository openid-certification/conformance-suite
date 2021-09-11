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
 * This is validator for API - Cartão de Crédito "Lista de cartões de crédito"
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#lista-de-cartoes-de-credito
 **/

@ApiName("Card List")
public class CardListResponseResponseValidator extends AbstractJsonAssertingCondition {

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
			//	.setPattern("\\w*\\W*") //TODO wrong pattern
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
				//.setPattern("\\w*\\W*") //TODO wrong pattern
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
