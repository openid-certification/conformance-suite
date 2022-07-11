package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDateTime;

public class AddBookingDateSixMonthsBefore extends AbstractAddBookingDateParameters {
	@Override
	@PreEnvironment(strings = "accountId")
	@PostEnvironment(strings = {"fromBookingDate","toBookingDate"})
	public Environment evaluate(Environment env) {
		String fromBookingDate = formatDateParam(LocalDateTime.now().minusDays(180));
		String toBookingDate = formatDateParam(LocalDateTime.now());

		addBookingDateParamsToAccountsEndpoint(env, fromBookingDate,toBookingDate,"/accounts/%s/transactions?fromBookingDate=%s&toBookingDate=%s");

		return super.evaluate(env);
	}
}
