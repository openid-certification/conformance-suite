package net.openid.conformance.openbanking_brasil.testmodules.support;

import java.time.Duration;
import java.time.LocalDate;

public class EnsureTransactionsDateIsNoOlderThan7Days extends ValidateTransactionsDate {

	private String errorMessage = "Transaction is older than 7 days";

	@Override
	protected boolean isDateInvalid(LocalDate currentDate, LocalDate transactionDate) {
		long differenceInDays = Duration.between(transactionDate.atStartOfDay(), currentDate.atStartOfDay()).toDays();
		if (differenceInDays < 0) {
			this.errorMessage = "Transaction cannot be in future";
		}
		return differenceInDays > 6 || differenceInDays < 0;
	}

	@Override
	protected String getErrorMessage() {
		return errorMessage;
	}


}
