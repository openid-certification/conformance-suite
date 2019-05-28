package io.fintechlabs.testframework.condition.client;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateCreateAccountRequestRequestWithExpiration extends AbstractCondition {

	public CreateCreateAccountRequestRequestWithExpiration(String testId, TestInstanceEventLog log,
			ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.
	 * testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment()
	@PostEnvironment(required = "account_requests_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonArray permissions = new JsonArray();
		permissions.add("ReadAccountsBasic");

		JsonObject data = new JsonObject();
		data.add("Permissions", permissions);

		Instant baseDateRough = Instant.now();
		Instant baseDate = baseDateRough.minusNanos(baseDateRough.getNano());

		String expirationDateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
				.format(baseDate.plus(2, ChronoUnit.HOURS));
		String fromDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
				.format(baseDate.minus(30, ChronoUnit.DAYS));
		String toDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
				.format(baseDate);

		data.addProperty("ExpirationDateTime", expirationDateTime);
		data.addProperty("TransactionFromDateTime", fromDate);
		data.addProperty("TransactionToDateTime", toDate);

		JsonObject o = new JsonObject();
		o.add("Data", data);
		o.add("Risk", new JsonObject());

		env.putObject("account_requests_endpoint_request", o);

		logSuccess(args("account_requests_endpoint_request", o));

		return env;
	}

}
