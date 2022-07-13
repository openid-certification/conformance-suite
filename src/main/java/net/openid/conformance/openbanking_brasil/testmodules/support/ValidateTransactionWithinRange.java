package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

public class ValidateTransactionWithinRange extends AbstractCondition {


	@Override
	@PostEnvironment(strings = "transactionDate")
	@PreEnvironment(strings = {"toBookingDate", "fromBookingDate"}, required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		String fromBookingDate = env.getString("fromBookingDate");
		String toBookingDate = env.getString("toBookingDate");

		JsonElement response = env.getElementFromObject("resource_endpoint_response_full","body.data");
		if (response == null){
			throw error("No transactions returned unable to validate the defined behaviour with booking date query parameters",
				args("response", env.getObject("resource_endpoint_response_full"),
					"body", env.getElementFromObject("resource_endpoint_response_full","body"),
					"data", env.getElementFromObject("resource_endpoint_response_full","body.data")));
		}
		JsonArray transactions = response.getAsJsonArray();

		int amountOfTransactions = transactions.size();
		Random random = new Random();
		JsonElement randomTransaction = transactions.get(random.nextInt(amountOfTransactions));

		JsonObject randomTransactionObject = randomTransaction.getAsJsonObject();

		String randomTransactionDate = OIDFJSON.getString(randomTransactionObject.get("transactionDate"));

		validateTransactionDate(randomTransactionDate, fromBookingDate,toBookingDate);
		logSuccess("Booking date parameters successfully validated to be within the specified range");
		env.putString("transactionDate",randomTransactionDate);
		return env;
	}

	protected void validateTransactionDate(String transactionDate, String fromBookingDate, String toBookingDate){
		LocalDate fmtTransactionDate = parseStringToDate(transactionDate);
		LocalDate fmtFromBookingDate = parseStringToDate(fromBookingDate);
		LocalDate fmtToBookingDate   = parseStringToDate(toBookingDate);

		if (fmtTransactionDate.isAfter(fmtToBookingDate) || fmtTransactionDate.isBefore(fmtFromBookingDate)){
			throw error("Transaction returns is not within the range of the specified query parameters", Map.of("Date: ", transactionDate));
		}
	}


	protected LocalDate parseStringToDate(String dateToParse){
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		return LocalDate.parse(dateToParse, dateFormat);
	}


}
