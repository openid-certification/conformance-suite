package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class AbstractCheckExpectedDateResponse extends AbstractCondition {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	@PreEnvironment(required = {"resource_endpoint_response_full", "full_range_response"}) // Works with CopyResourceEndpointResponse
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

		if(fullRangeData.isEmpty()){
			throw error("Full Range data response cannot be empty");
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
			if (!date.isAfter(toDate) && !date.isBefore(fromDate)) {
				expectedData.add(resourceObject);
			}
		});


		if(!expectedData.isEmpty() && actualData.isEmpty()){
			throw error("The returned data array is not what was expected", args("Returned", actualData, "Expected", expectedData));
		}

		expectedData.iterator().forEachRemaining(expectedResource -> {
			if (!actualData.contains(expectedResource.getAsJsonObject())) {
				throw error("The returned data array does not contain the expected resource", args("Returned", actualData, "Expected", expectedResource));
			}
		});

		logSuccess("The returned data array has expected resources", args("Returned", actualData, "Expected", expectedData));

		return env;
	}

	protected abstract String getToDateName();

	protected abstract String getFromDateName();


}