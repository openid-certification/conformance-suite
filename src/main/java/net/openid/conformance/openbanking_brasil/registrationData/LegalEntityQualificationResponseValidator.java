package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API-Dados Cadastrais "Qualificação Pessoa Jurídica"
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#qualificacao-pessoa-juridica">Qualificação Pessoa Jurídica</a>
 **/

@ApiName("Legal Entity Qualification")
public class LegalEntityQualificationResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data.updateDateTime");

		assertHasField(body, "$.data.economicActivities[0]");
		assertHasLongField(body, "$.data.economicActivities[0].code");
		assertHasBooleanField(body, "$.data.economicActivities[0].isMain");

		assertHasField(body, "$.data.informedRevenue");
		assertHasStringField(body, "$.data.informedRevenue.frequency");
		assertHasDoubleField(body, "$.data.informedRevenue.amount");
		assertHasStringField(body, "$.data.informedRevenue.currency");
		assertHasStringField(body, "$.data.informedRevenue.frequencyAdditionalInfo");
		assertHasIntField(body, "$.data.informedRevenue.year");

		assertHasField(body, "$.data.informedPatrimony");
		assertHasDoubleField(body, "$.data.informedPatrimony.amount");
		assertHasStringField(body, "$.data.informedPatrimony.currency");
		assertHasStringField(body, "$.data.informedPatrimony.date");

		return environment;
	}
}
