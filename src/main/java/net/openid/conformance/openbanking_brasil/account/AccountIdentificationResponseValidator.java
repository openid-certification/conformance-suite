package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API-Contas "Lista de contas"
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#lista-de-contas">Lista de contas</a>
 **/
@ApiName("Account Identification")
public class AccountIdentificationResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data.compeCode");
		assertHasStringField(body, "$.data.branchCode");
		assertHasStringField(body, "$.data.number");
		assertHasStringField(body, "$.data.checkDigit");
		assertHasStringField(body, "$.data.type");
		assertHasStringField(body, "$.data.subtype");
		assertHasStringField(body, "$.data.currency");

		return environment;
	}
}
