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
 * This is validator for API-Cartão de Crédito | Limites de cartão de crédito
 * https://openbanking-brasil.github.io/areadesenvolvedor/#limites-de-cartao-de-credito
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
				.setEnums(enumCreditLimitType)
				.build());

		assertField(data,
			new StringField
				.Builder("consolidationType")
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
				.setEnums(enumLineName)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("lineNameAdditionalInfo")
				//.setPattern("\\w*\\W*") //TODO wrong pattern
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
				.setMaxLength(20)
				.setMinLength(0)
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
				.setMaxLength(20)
				.setMinLength(0)
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
				.build());
	}
}
