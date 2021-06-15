package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API-Dados Cadastrais | Qualificação Pessoa Natural
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#identificacao-pessoa-juridica">Qualificação Pessoa Natural</a>
 */

@ApiName("Natural Personal Qualification")
public class NaturalPersonalQualificationResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data.updateDateTime");
		assertHasStringField(body, "$.data.companyCnpj");
		assertHasStringField(body, "$.data.occupationCode");
		assertHasStringField(body, "$.data.occupationDescription");

		assertHasField(body, "$.data.informedIncome");
		assertHasStringField(body, "$.data.informedIncome.frequency");
		assertHasDoubleField(body, "$.data.informedIncome.amount");
		assertHasStringField(body, "$.data.informedIncome.currency");
		assertHasStringField(body, "$.data.informedIncome.date");

		assertHasField(body, "$.data.informedPatrimony");
		assertHasDoubleField(body, "$.data.informedPatrimony.amount");
		assertHasStringField(body, "$.data.informedPatrimony.currency");
		assertHasIntField(body, "$.data.informedPatrimony.year");

		return environment;
	}
}
