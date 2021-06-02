package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API-Contas| Transações da contas
 * https://openbanking-brasil.github.io/areadesenvolvedor/#transacoes-da-conta
 */
public class AccountTransactionsValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body,"$.data[0].completedAuthorisedPaymentType");
		assertHasStringField(body,"$.data[0].creditDebitType");
		assertHasStringField(body,"$.data[0].transactionName");
		assertHasStringField(body,"$.data[0].type");
		assertHasDoubleField(body,"$.data[0].amount");
		assertHasStringField(body,"$.data[0].transactionCurrency");
		assertHasStringField(body,"$.data[0].transactionDate");
		assertHasStringField(body,"$.data[0].partiePersonType");
		assertHasStringField(body,"$.data[0].partieCompeCode");
		assertHasStringField(body,"$.data[0].partieBranchCode");
		assertHasStringField(body,"$.data[0].partieNumber");
		assertHasStringField(body,"$.data[0].partieCheckDigit");
		assertHasStringField(body,"$.data[0].transactionId");

		return environment;
	}
}
