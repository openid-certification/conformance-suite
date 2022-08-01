package net.openid.conformance.openbanking_brasil.creditCard.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api: swagger/openBanking/swagger-credit-cards-api-V2.yaml
 * Api endpoint: /accounts/{creditCardAccountId}/limits
 * Api version: 2.0.1.final
 **/

@ApiName("Credit Card Accounts Limits V2")
public class CreditCardAccountsLimitsResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> ENUM_CREDIT_LIMIT_TYPE = SetUtils.createSet("LIMITE_CREDITO_TOTAL, LIMITE_CREDITO_MODALIDADE_OPERACAO");
	public static final Set<String> ENUM_CONSOLIDATION_TYPE = SetUtils.createSet("CONSOLIDADO, INDIVIDUAL");
	private static final Set<String> ENUM_LINE_NAME = SetUtils.createSet("CREDITO_A_VISTA, CREDITO_PARCELADO, SAQUE_CREDITO_BRASIL, SAQUE_CREDITO_EXTERIOR, EMPRESTIMO_CARTAO_CONSIGNADO, OUTROS");


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

	private void assertData(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("creditLineLimitType")
				.setEnums(ENUM_CREDIT_LIMIT_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("consolidationType")
				.setEnums(ENUM_CONSOLIDATION_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("identificationNumber")
				.setMaxLength(100)
				.setMinLength(1)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.build());

		assertField(data,
			new StringField
				.Builder("lineName")
				.setEnums(ENUM_LINE_NAME)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("lineNameAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.setOptional()
				.build());

		assertField(data,
			new BooleanField
				.Builder("isLimitFlexible")
				.build());

		assertField(data,
			new ObjectField
				.Builder("limitAmount")
				.setValidator(this::assertAmount)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("usedAmount")
				.setValidator(this::assertUsedAmount)
				.build());

		assertField(data,
			new ObjectField
				.Builder("availableAmount")
				.setValidator(this::assertAvailableAmount)
				.setOptional()
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

	private void assertUsedAmount(JsonObject data) {
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

	private void assertAvailableAmount(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("amount")
				.setMinLength(4)
				.setMaxLength(21)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setMaxLength(3)
				.setOptional()
				.build());
	}
}
