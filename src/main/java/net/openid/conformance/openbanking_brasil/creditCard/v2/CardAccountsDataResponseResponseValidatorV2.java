package net.openid.conformance.openbanking_brasil.creditCard.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openBanking/swagger-credit-cards-api-V2.yaml
 * Api endpoint: /accounts
 * Api version: 2.0.0-RC1.0
 **/

@ApiName("Card List V2")
public class CardAccountsDataResponseResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> ENUM_PRODUCT_TYPE = SetUtils.createSet("CLASSIC_NACIONAL, CLASSIC_INTERNACIONAL, GOLD, PLATINUM, INFINITE, ELECTRON, STANDARD_NACIONAL, STANDARD_INTERNACIONAL, ELETRONIC, BLACK, REDESHOP, MAESTRO_MASTERCARD_MAESTRO, GREEN, BLUE, BLUEBOX, PROFISSIONAL_LIBERAL, CHEQUE_ELETRONICO, CORPORATIVO, EMPRESARIAL, COMPRAS, BASICO_NACIONAL, BASICO_INTERNACIONAL, NANQUIM, GRAFITE, MAIS, OUTROS");
	public static final Set<String> ENUM_CREDIT_CARD_NETWORK = SetUtils.createSet("VISA, MASTERCARD, AMERICAN_EXPRESS, DINERS_CLUB, HIPERCARD, BANDEIRA_PROPRIA, CHEQUE_ELETRONICO, ELO, OUTRAS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.setMinItems(0)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("creditCardAccountId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
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
				.setPattern("^\\d{14}$")
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
				.setEnums(ENUM_CREDIT_CARD_NETWORK)
				.build());

		assertField(body,
			new StringField
				.Builder("networkAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.setOptional()
				.build());
	}
}
