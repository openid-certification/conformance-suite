package net.openid.conformance.openbanking_brasil.creditCard.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openBanking/swagger-credit-cards-api-V2.yaml
 * Api endpoint: /accounts/{creditCardAccountId}
 * Api version: 2.0.1.final
 **/

@ApiName("Card Identification V2")
public class CardIdentificationResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> ENUM_PRODUCT_TYPE = SetUtils.createSet("CLASSIC_NACIONAL, CLASSIC_INTERNACIONAL, GOLD, PLATINUM, INFINITE, ELECTRON, STANDARD_NACIONAL, STANDARD_INTERNACIONAL, ELETRONIC, BLACK, REDESHOP, MAESTRO_MASTERCARD_MAESTRO, GREEN, BLUE, BLUEBOX, PROFISSIONAL_LIBERAL, CHEQUE_ELETRONICO, CORPORATIVO, EMPRESARIAL, COMPRAS, BASICO_NACIONAL, BASICO_INTERNACIONAL, NANQUIM, GRAFITE, MAIS, OUTROS");
	public static final Set<String> ENUM_CREDIT_CARD_NETWORK = SetUtils.createSet("VISA, MASTERCARD, AMERICAN_EXPRESS, DINERS_CLUB, HIPERCARD, BANDEIRA_PROPRIA, CHEQUE_ELETRONICO, ELO, OUTRAS");

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
			new StringField
				.Builder("name")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.build());

		assertField(data,
			new StringField
				.Builder("productType")
				.setEnums(ENUM_PRODUCT_TYPE)
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
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.setMinLength(1)
				.setMaxLength(100)
				.build());

		assertField(jsonObject,
			new BooleanField
				.Builder("isMultipleCreditCard")
				.build());
	}
}
