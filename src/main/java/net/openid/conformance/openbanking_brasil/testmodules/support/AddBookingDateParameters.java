package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class AddBookingDateParameters extends ResourceBuilder {
	@Override
	@PreEnvironment(strings = {"resource_endpoint_response", "accountId"})
	public Environment evaluate(Environment env){

		LocalDateTime date = LocalDateTime.now();
		LocalDateTime fromDate = date.minusMonths(12);
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String fromDateF = fromDate.format(dateFormat);
		LocalDateTime toDate = date;
		String toDateF = toDate.format(dateFormat);

		String accountId = env.getString("accountId");

		setApi("accounts");
		setEndpoint(String.format("/accounts/%s/transactions?fromBookingDate=%s&toBookingDate=%s", accountId, fromDateF, toDateF));

		String data = env.getString("resource_endpoint_response");
		JsonObject checkObject = new JsonParser().parse(data).getAsJsonObject();
		JsonArray checkArray = checkObject.getAsJsonArray("data");
		try {
			int i = 1;
			boolean olderThan = false;
			boolean newerThan = false;
			if (checkArray.get(1) != null) {
				while(i < checkArray.size()) {
					var dataElement1 = checkArray.get(0);
					var dataElement2 = checkArray.get(i);
					JsonObject dataObject1 = dataElement1.getAsJsonObject();
					JsonObject dataObject2 = dataElement2.getAsJsonObject();

					String transactionDate1 = OIDFJSON.getString(dataObject1.get("transactionDate"));
					String transactionDate2 = OIDFJSON.getString(dataObject2.get("transactionDate"));
					log("Transaction Date 1: " + transactionDate1 + " Transaction Date 2: " + transactionDate2);

					LocalDate transaction1Date = LocalDate.parse(transactionDate1);
					LocalDate transaction2Date = LocalDate.parse(transactionDate2);

					LocalDate checkDate = LocalDate.now().minusMonths(6);
					long transaction1Difference = ChronoUnit.MONTHS.between(transaction1Date, checkDate);
					long transaction2Difference = ChronoUnit.MONTHS.between(transaction2Date, checkDate);

					if (transaction1Difference > 6 || transaction2Difference > 6) {
						olderThan = true;
						if (transaction1Difference <= 6 || transaction2Difference <= 6) {
							newerThan = true;
						}
					}
					i = i+1;
				}
				if (olderThan == true && newerThan == true){
					logSuccess("At least one transaction date is older than 6 months and at least one transaction date is 6 months or less");
				}
				else if (olderThan == false ){
					logFailure("At least one transaction date should be older than 6 months");
				}
				else if (newerThan == false){
					logFailure("At least one transaction date should be 6 months or less");
				}
			}
		}
		catch (IndexOutOfBoundsException e){
			log("Less than 2 transactions returned. Cannot compare transaction dates");
		}

		Duration duration = Duration.between(fromDate, toDate);
		var days = duration.toDays();
		if(days==365){
			log("Query parameters  are 1 year apart");
		}
		else {
			log("Query parameters " + days + " days apart");
		}
		return super.evaluate(env);
	}
}
