package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API-Dados Cadastrais "Identificacao pessoa natural"
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#identificacao-pessoa-natural">Identificacao pessoa natural</a>
 **/

@ApiName("Natural Person Relationship")
public class NaturalPersonRelationshipResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data.updateDateTime");
		assertHasStringField(body, "$.data.startDate");
		assertHasField(body, "$.data.procurators");
		assertHasStringField(body, "$.data.procurators[0].type");
		assertHasStringField(body, "$.data.procurators[0].cpfNumber");
		assertHasStringField(body, "$.data.procurators[0].civilName");
		assertHasStringField(body, "$.data.procurators[0].socialName");
		assertHasField(body, "$.data.productsServicesType");
		assertHasStringField(body, "$.data.productsServicesType[0]");
		assertHasField(body, "$.data.accounts");
		assertHasStringField(body, "$.data.accounts[0].compeCode");
		assertHasStringField(body, "$.data.accounts[0].branchCode");
		assertHasStringField(body, "$.data.accounts[0].number");
		assertHasStringField(body, "$.data.accounts[0].checkDigit");
		assertHasStringField(body, "$.data.accounts[0].type");
		assertHasStringField(body, "$.data.accounts[0].subtype");
		assertHasStringField(body, "$.data.productsServicesTypeAdditionalInfo");

		return environment;
	}
}
