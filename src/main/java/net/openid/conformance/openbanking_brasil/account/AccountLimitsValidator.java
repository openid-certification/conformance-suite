package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * This validates Account Limits
 * https://openbanking-brasil.github.io/areadesenvolvedor/#limites-da-conta
 * */
public class AccountLimitsValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasDoubleField(body, "$.data.overdraftContractedLimit");
		assertHasStringField(body, "$.data.overdraftContractedLimitCurrency");
		assertHasDoubleField (body, "$.data.overdraftUsedLimit");
		assertHasStringField(body, "$.data.overdraftUsedLimitCurrency");
		assertHasDoubleField(body, "$.data.unarrangedOverdraftAmount");
		assertHasStringField(body, "$.data.unarrangedOverdraftAmountCurrency");
		return environment;
	}
}
