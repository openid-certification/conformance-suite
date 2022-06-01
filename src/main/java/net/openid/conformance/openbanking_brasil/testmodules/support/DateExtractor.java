package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class DateExtractor extends AbstractCondition {
	@Override
	@PreEnvironment(strings = {"resource_endpoint_response","base_resource_url", "accountId"})
	@PostEnvironment(strings = {"transactionDate","base_resource_url"})
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		String request = env.getString("base_resource_url");
		JsonObject consent = new JsonParser().parse(entityString).getAsJsonObject();
		JsonArray data = consent.getAsJsonArray("data");
		var dataElement = data.get(0);
		JsonObject dataObject = dataElement.getAsJsonObject();
		String transactionDate = OIDFJSON.getString(dataObject.get("transactionDate"));
		env.putString("transactionDate", transactionDate);
		logSuccess("Transaction Date", args("transactionDate", transactionDate));

		if (dataObject.get("transactionId") != null) {
			String transactionId = OIDFJSON.getString(dataObject.get("transactionId"));
			env.putString("transactionId", transactionId);
			logSuccess("Transaction ID", args("transactionId", transactionId));
		}

		String newRequest = null;
		if (request.contains("fromBookingDate")) {
			int oldUrl = request.indexOf("?");
			newRequest = request.substring(0, oldUrl);
		}

		var url = String.format(newRequest + "?fromBookingDate=%s&toBookingDate=%s", transactionDate, transactionDate);
		logSuccess("Added fromBookingDate and toBookingDate query parameters: " + url);
		env.putString("base_resource_url", url);

		return env;
	}
}