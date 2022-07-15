package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.*;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

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

		String bodyJsonString = env.getString("resource_endpoint_response_full", "body");

		if (Strings.isNullOrEmpty(bodyJsonString)) {
			throw error("Body element is missing in the resource_endpoint_response_full");
		}

		JsonObject body;

		try {
			Gson gson = JsonUtils.createBigDecimalAwareGson();
			body = gson.fromJson(bodyJsonString, JsonObject.class);

		} catch (JsonSyntaxException e) {
			throw error("Body is not json", args("body", bodyJsonString));
		}

		JsonArray transactions = body.getAsJsonArray("data");

		if (transactions == null || transactions.isEmpty()) {
			throw error("No transactions returned unable to validate the defined behaviour with booking date query parameters",
				args("response", env.getObject("resource_endpoint_response_full"),
					"body", body,
					"data", transactions));
		}

		int amountOfTransactions = transactions.size();
		Random random = new Random();
		JsonElement randomTransaction = transactions.get(random.nextInt(amountOfTransactions));

		JsonObject randomTransactionObject = randomTransaction.getAsJsonObject();

		String randomTransactionDate = OIDFJSON.getString(randomTransactionObject.get("transactionDate"));

		validateTransactionDate(randomTransactionDate, fromBookingDate, toBookingDate);
		logSuccess("Booking date parameters successfully validated to be within the specified range");
		env.putString("transactionDate", randomTransactionDate);


		return env;
	}

	protected void validateTransactionDate(String transactionDate, String fromBookingDate, String toBookingDate) {
		LocalDate fmtTransactionDate = parseStringToDate(transactionDate);
		LocalDate fmtFromBookingDate = parseStringToDate(fromBookingDate);
		LocalDate fmtToBookingDate = parseStringToDate(toBookingDate);

		if (fmtTransactionDate.isAfter(fmtToBookingDate) || fmtTransactionDate.isBefore(fmtFromBookingDate)) {
			throw error("Transaction returns is not within the range of the specified query parameters", Map.of("Date: ", transactionDate));
		}
	}


	protected LocalDate parseStringToDate(String dateToParse) {
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		return LocalDate.parse(dateToParse, dateFormat);
	}


}
