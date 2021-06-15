package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Cartão de Crédito | Fatura de Cartão de Crédito
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#fatura-de-cartao-de-credito">Fatura de Cartão de Crédito</a>
 **/
@ApiName("Credit Card Bill")
public class CreditCardBillValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body,"$.data[0].billId");
		assertHasStringField(body,"$.data[0].dueDate");
		assertHasDoubleField(body,"$.data[0].billTotalAmount");
		assertHasStringField(body,"$.data[0].billTotalAmountCurrency");
		assertHasDoubleField(body,"$.data[0].billMinimumAmount");
		assertHasStringField(body,"$.data[0].billMinimumAmountCurrency");
		assertHasBooleanField(body,"$.data[0].isInstalment");

		assertHasField(body,"$.data[0].financeCharges[0]");
		assertHasStringField(body,"$.data[0].financeCharges[0].type");
		assertHasStringField(body,"$.data[0].financeCharges[0].additionalInfo");
		assertHasDoubleField(body,"$.data[0].financeCharges[0].amount");
		assertHasStringField(body,"$.data[0].financeCharges[0].currency");

		assertHasField(body,"$.data[0].payments[0]");
		assertHasStringField(body,"$.data[0].payments[0].valueType");
		assertHasStringField(body,"$.data[0].payments[0].paymentDate");
		assertHasStringField(body,"$.data[0].payments[0].paymentMode");
		assertHasDoubleField(body,"$.data[0].payments[0].amount");
		assertHasStringField(body,"$.data[0].payments[0].currency");

		return environment;
	}
}
