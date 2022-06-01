package net.openid.conformance.openbanking_brasil.testmodules.support;

import java.time.LocalDate;

public class EnsureTransactionsDateIsSetToToday extends ValidateTransactionsDate {

	@Override
	protected boolean isDateInvalid(LocalDate currentDate, LocalDate transactionDate) {
		return !currentDate.isEqual(transactionDate);
	}

	@Override
	protected String getErrorMessage() {
		return "The date of the first transaction is not today's date";
	}
}
