package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.client.utils.DateUtils;
import org.springframework.http.HttpHeaders;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;


public class AddBookingDateHeaders extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_request_headers" )
	//@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env){
		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		//Date fromDate = new Date();
		//headers.addProperty("fromBookingDate", DateUtils.formatDate(fromDate));
		LocalDateTime fromDate = LocalDateTime.now();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss");
		String fromDateF = fromDate.format(dateFormat) + " GMT"; //This will always say timezone is GMT even if it is not
		headers.addProperty("fromBookingDate", String.valueOf(fromDateF));


		//TODO This date needs to have different values for different tests
		//----Using Date works best but uses depreciated functions----
		//var toDate = new Date();
		//toDate.setDate(toDate.getDate() + 365); //Is the fact these are depreciated an issue?
		//headers.addProperty("toBookingDate", DateUtils.formatDate(toDate));

		//----LocalDateTime works without depreciated functions but may cause logic issues----
		LocalDateTime toDate = LocalDateTime.now().plusMonths(12); //This date will not always be valid for the tests
		//DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss");
		String toDateF = toDate.format(dateFormat) + " GMT";  //This will always say timezone is GMT even if it is not
		headers.addProperty("toBookingDate", String.valueOf(toDateF));


		//----------------------
		//TODO This will need to go in the actual test class
		Duration duration = Duration.between(fromDate, toDate);
		var days = duration.toDays();
		//TODO This if condition will need to be corrected to match the tests
		if(days==365){
			log("Payments are 1 year apart");
		}
		else {
			log("Payments are " + days + " days apart");
		}
		//-----------------------

		log("Added fromBookingDate and toBookingDate headers", headers);
		return env;
	}

	protected HttpHeaders  getHeaders(Environment env) {
		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");
		log("Using request headers: " + requestHeaders);
		HttpHeaders headers = headersFromJson(requestHeaders);

		headers.set("fromBookingDate", "toBookingDate");
		return headers;
	}
}
