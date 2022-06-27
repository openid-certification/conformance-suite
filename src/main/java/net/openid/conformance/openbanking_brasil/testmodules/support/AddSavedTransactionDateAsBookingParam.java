package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSavedTransactionDateAsBookingParam extends AbstractAddBookingDateParameters{
	@PreEnvironment(strings = {"accountId","transactionDate"})
	@PostEnvironment(strings = {"fromBookingDate","toBookingDate"})
	public Environment evaluate(Environment env) {
		String fromBookingDate = env.getString("transactionDate");
		String toBookingDate = env.getString("transactionDate");

		addBookingDateParamsToAccountsEndpoint(env, fromBookingDate,toBookingDate);

		return super.evaluate(env);
	}
}
