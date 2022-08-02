package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class AbstractCheckExpectedDateResponse extends AbstractCondition {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public Environment evaluate(Environment env) {

		Gson gson = JsonUtils.createBigDecimalAwareGson();
		JsonObject actualResponse = gson.fromJson(env.getString("resource_endpoint_response_full", "body"), JsonObject.class);
		JsonObject fullRangeResponse = gson.fromJson(env.getString("full_range_response", "body"), JsonObject.class);

		LocalDate fromDate = LocalDate.parse(env.getString(getFromDateName()), FORMATTER);
		LocalDate toDate = LocalDate.parse(env.getString(getToDateName()), FORMATTER);

		JsonArray fullRangeData = fullRangeResponse.getAsJsonArray("data");
		JsonArray actualData = actualResponse.getAsJsonArray("data");

		if (fullRangeData == null) {
			throw error("Could not find data JSON array in the full range response", args("fullRangeResponse", fullRangeResponse));
		}

		if (actualData == null) {
			throw error("Could not find data JSON array in the actual response", args("actualResponse", actualResponse));

		}

		JsonArray expectedData = new JsonArray();

		fullRangeData.iterator().forEachRemaining(resource -> {
			JsonObject resourceObject = resource.getAsJsonObject();
			JsonElement resourceDate = resourceObject.get("transactionDate");
			if (resourceDate == null) {
				throw error("Could no find transactionDate JSON element in the resource Object", args("resource", resource));
			}
			LocalDate date = LocalDate.parse(OIDFJSON.getString(resourceDate), FORMATTER);
			if (date.isAfter(fromDate) && (date.isBefore(toDate) || date.isEqual(toDate))) {
				expectedData.add(resourceObject);
			}
		});

		if (!actualData.equals(expectedData)) {
			throw error("The returned data array is not what was expected", args("Returned", actualData, "Expected", expectedData));
		}

		logSuccess("The returned data array has expected resources", args("Returned", actualData, "Expected", expectedData));

		return env;
	}

	protected abstract String getToDateName();

	protected abstract String getFromDateName();


}
