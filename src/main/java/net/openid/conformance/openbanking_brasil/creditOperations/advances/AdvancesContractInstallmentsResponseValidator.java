package net.openid.conformance.openbanking_brasil.creditOperations.advances;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Operações de Crédito - Adiantamento a Depositantes  | Contract Installments
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#adiantamento-a-depositantes-parcelas-do-contrato
 */

@ApiName("Advances Contract Installments")
public class AdvancesContractInstallmentsResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data.typeNumberOfInstalments");
		assertHasLongField(body, "$.data.totalNumberOfInstalments");
		assertHasStringField(body, "$.data.typeContractRemaining");
		assertHasLongField(body, "$.data.contractRemainingNumber");
		assertHasLongField(body, "$.data.paidInstalments");
		assertHasLongField(body, "$.data.dueInstalments");
		assertHasLongField(body, "$.data.pastDueInstalments");

		assertHasField(body, "$.data.balloonPayments");
		assertHasField(body, "$.data.balloonPayments[0]");
		assertHasStringField(body, "$.data.balloonPayments[0].dueDate");
		assertHasStringField(body, "$.data.balloonPayments[0].currency");
		assertHasDoubleField(body, "$.data.balloonPayments[0].amount");

		return environment;
	}
}
