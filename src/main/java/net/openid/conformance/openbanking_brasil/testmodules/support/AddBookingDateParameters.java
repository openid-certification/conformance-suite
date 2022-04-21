package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddBookingDateParameters extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response" )
	public Environment evaluate(Environment env){
		String request = env.getString("resource_endpoint_response");
		JsonObject consent = new JsonParser().parse(request).getAsJsonObject();
		JsonArray data = consent.getAsJsonArray("data");
		var dataElement = data.get(0);
		JsonObject dataObject = dataElement.getAsJsonObject();

		LocalDateTime fromDate = LocalDateTime.now();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss");
		String fromDateF = fromDate.format(dateFormat);
		LocalDateTime toDate = LocalDateTime.now().plusMonths(12);
		String toDateF = toDate.format(dateFormat);

		dataObject.addProperty("fromBookingDate",fromDateF);
		dataObject.addProperty("toBookingDate", toDateF);
		log("Added fromBookingDate and toBookingDate query parameters " + data);

		Duration duration = Duration.between(fromDate, toDate);
		var days = duration.toDays();
		if(days==365){
			log("Payments are 1 year apart");
		}
		else {
			log("Payments are " + days + " days apart");
		}
		return env;
	}
}
