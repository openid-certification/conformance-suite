package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger_credit_cards_apis.yaml
 * Api endpoint: /accounts/{creditCardAccountId}/limits
 * Api git hash: 91e2ff8327cb35eb1ae571c7b2264e6173b34eeb
 */
@ApiName("Credit Card Accounts Limits")
public class CreditCardAccountsLimitsResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);

		return environment;
	}

	private void assertInnerFields(JsonObject data) {
		Set<String> enumCreditLimitType = Sets.newHashSet("LIMITE_CREDITO_TOTAL", "LIMITE_CREDITO_MODALIDADE_OPERACAO");
		Set<String> enumConsolidationType = Sets.newHashSet("CONSOLIDADO", "INDIVIDUAL");
		Set<String> enumLineName = Sets.newHashSet("CREDITO_A_VISTA", "CREDITO_PARCELADO", "SAQUE_CREDITO_BRASIL", "SAQUE_CREDITO_EXTERIOR", "EMPRESTIMO_CARTAO_CONSIGNADO", "OUTROS");

		assertField(data,
			new StringField
				.Builder("creditLineLimitType")
				.setMaxLength(34)
				.setEnums(enumCreditLimitType)
				.build());

		assertField(data,
			new StringField
				.Builder("consolidationType")
				.setMaxLength(11)
				.setEnums(enumConsolidationType)
				.build());

		assertField(data,
			new StringField
				.Builder("identificationNumber")
				.setMaxLength(100)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.build());

		assertField(data,
			new StringField
				.Builder("lineName")
				.setMaxLength(28)
				.setEnums(enumLineName)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("lineNameAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(data,
			new BooleanField
				.Builder("isLimitFlexible")
				.build());

		assertField(data,
			new StringField
				.Builder("limitAmountCurrency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("limitAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());


		assertField(data,
			new StringField
				.Builder("usedAmountCurrency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("usedAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("availableAmountCurrency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DoubleField
				.Builder("availableAmount")
				.setMaxLength(20)
				.setMinLength(0)
				.setNullable()
				.build());
	}
}
