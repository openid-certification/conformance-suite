package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class AddBookingDateHeaders extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_request_headers" )
	public Environment evaluate(Environment env){
		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		LocalDateTime fromDate = LocalDateTime.now();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss");
		String fromDateF = fromDate.format(dateFormat);
		headers.addProperty("fromBookingDate", fromDateF);
		LocalDateTime toDate = LocalDateTime.now().plusMonths(12);
		String toDateF = toDate.format(dateFormat);
		headers.addProperty("toBookingDate", toDateF);
		Duration duration = Duration.between(fromDate, toDate);
		var days = duration.toDays();
		if(days==365){
			log("Payments are 1 year apart");
		}
		else {
			log("Payments are " + days + " days apart");
		}
		log("Added fromBookingDate and toBookingDate headers", headers);
		return env;
	}

	protected HttpHeaders getHeaders(Environment env) {
		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");
		log("Using request headers: " + requestHeaders);
		HttpHeaders headers = headersFromJson(requestHeaders);
		headers.set("fromBookingDate", "toBookingDate");
		return headers;
	}
}
