package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddBookingDateParameters extends AbstractCondition {
	@Override
	@PreEnvironment(strings = {"resource_endpoint_response","base_resource_url", "accountId"})

	public Environment evaluate(Environment env){
		String request = env.getString("base_resource_url");

		LocalDateTime fromDate = LocalDateTime.now();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String fromDateF = fromDate.format(dateFormat);
		LocalDateTime toDate = LocalDateTime.now().plusMonths(12);
		String toDateF = toDate.format(dateFormat);

		String accountId = env.getString("accountId");
		var url = String.format(request + "/%s/transactions?fromBookingDate=%s&toBookingDate=%s",accountId,fromDateF, toDateF);
		log("Added fromBookingDate and toBookingDate query parameters to URL: " + url);

		String data = env.getString("resource_endpoint_response");
		log("Returned Transactions: " + data);

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
