package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public abstract class ValidateTransactionsDate extends AbstractCondition {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {

		JsonArray data = env.getElementFromObject("resource_endpoint_response_full", "body.data").getAsJsonArray();

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		data.forEach(jsonElement -> {
			JsonObject transactionObject = jsonElement.getAsJsonObject();
			String transactionDateString = OIDFJSON.getString(transactionObject.get("transactionDate"));
			LocalDate transactionDate = LocalDate.parse(transactionDateString, FORMATTER);

			if (isDateInvalid(currentDate, transactionDate)) {
				throw error(getErrorMessage(),
					Map.of("Transaction", transactionObject,
						"Current Date", currentDate.format(FORMATTER),
						"Transaction Date", transactionDate.format(FORMATTER)));
			}
		});

		logSuccess("All transactions dates are valid");

		return env;
	}

	abstract protected boolean isDateInvalid(LocalDate currentDate, LocalDate transactionDate);

	abstract protected String getErrorMessage();
}
