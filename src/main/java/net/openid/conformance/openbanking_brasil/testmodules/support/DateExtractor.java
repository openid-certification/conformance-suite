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
	@PreEnvironment(strings = "resource_endpoint_response")
	@PostEnvironment(strings = "transactionDate")
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		JsonObject consent = new JsonParser().parse(entityString).getAsJsonObject();
		JsonArray data = consent.getAsJsonArray("data");
		var dataElement = data.get(0);
		JsonObject dataObject = dataElement.getAsJsonObject();
		String transactionDate = OIDFJSON.getString(dataObject.get("transactionDate"));
		env.putString("transactionDate", transactionDate);
		logSuccess("Transaction Date", args("transactionDate", transactionDate));

		String transactionId = OIDFJSON.getString(dataObject.get("transactionId"));
		env.putString("transactionId", transactionId);
		logSuccess("Transaction ID", args("transactionId", transactionId));

		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		headers.addProperty("fromBookingDate", transactionDate);
		headers.addProperty("toBookingDate", transactionDate);
		logSuccess("Headers", headers);

		return env;
	}
}
