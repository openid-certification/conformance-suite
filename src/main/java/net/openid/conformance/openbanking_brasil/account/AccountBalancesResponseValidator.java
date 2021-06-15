package net.openid.conformance.openbanking_brasil.account;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.validation.RegexMatch;

/**
 * This is validator for API - Contas - Saldos da conta
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#saldos-da-conta">Saldos da conta</a>
 **/
@ApiName("Account Balances")
public class AccountBalancesResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertFieldData(body);
		assertFieldRegexp(body);
		return environment;
	}

	private void assertFieldData(JsonObject body) {
		assertHasDoubleField(body, "$.data.availableAmount");
		assertHasStringField(body, "$.data.availableAmountCurrency");
		assertHasDoubleField(body, "$.data.blockedAmount");
		assertHasStringField(body, "$.data.blockedAmountCurrency");
		assertHasLongField(body, "$.data.automaticallyInvestedAmount");
		assertHasStringField(body, "$.data.automaticallyInvestedAmountCurrency");
	}

	private void assertFieldRegexp(JsonObject body) {
		//TODO need to clarifi the regexps for availableAmount, blockedAmount, automaticallyInvestedAmount, automaticallyInvestedAmountCurrency
		assertJsonField(body, "$.data.availableAmount", RegexMatch.regex("(.+)"));
		assertJsonField(body, "$.data.availableAmountCurrency", RegexMatch.regex("^(\\w{3}){1}$"));
		assertJsonField(body, "$.data.blockedAmount", RegexMatch.regex(".+"));
		assertJsonField(body, "$.data.blockedAmountCurrency", RegexMatch.regex("^(\\w{3}){1}$"));
//		assertJsonField(body, "$.data.automaticallyInvestedAmount", RegexMatch.regex("([^.])+"));
		assertJsonField(body, "$.data.automaticallyInvestedAmountCurrency", RegexMatch.regex("([^.])+"));
	}
}
