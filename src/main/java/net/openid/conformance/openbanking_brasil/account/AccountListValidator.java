package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
/**This is validator for API-Contas|Lista de contas
 * https://openbanking-brasil.github.io/areadesenvolvedor/#lista-de-contas*/
public class AccountListValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body,"$.data[0].brandName");
		assertHasStringField(body,"$.data[0].companyCnpj");
		assertHasStringField(body,"$.data[0].type");
		assertHasStringField(body,"$.data[0].compeCode");
		assertHasStringField(body,"$.data[0].branchCode");
		assertHasStringField(body,"$.data[0].number");
		assertHasStringField(body,"$.data[0].checkDigit");
		assertHasStringField(body,"$.data[0].accountID");

		return environment;
	}
}
