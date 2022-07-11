package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractAddBookingDateParameters extends ResourceBuilder {

	protected void addBookingDateParamsToAccountsEndpoint(Environment environment, String fromBookingDate, String toBookingDate, String endpointPath){
		String accountId = environment.getString("accountId");
		addDatesToEnvironment(environment,fromBookingDate,toBookingDate);
		setApi("accounts");
		setEndpoint(String.format(endpointPath, accountId, fromBookingDate, toBookingDate));
	}

	protected String formatDateParam(LocalDateTime dateToFormat){
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		return dateToFormat.format(dateFormat);
	}

	protected void addDatesToEnvironment(Environment environment, String fromBookingDate, String toBookingDate){
		environment.putString("fromBookingDate",fromBookingDate);
		environment.putString("toBookingDate",toBookingDate);
	}
}
