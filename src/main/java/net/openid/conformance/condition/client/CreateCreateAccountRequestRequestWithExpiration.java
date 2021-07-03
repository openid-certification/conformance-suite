package net.openid.conformance.condition.client;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateCreateAccountRequestRequestWithExpiration extends AbstractCondition {

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

		Instant expiryTimeNoFractionalSeconds = baseDate.truncatedTo(ChronoUnit.SECONDS);

		String expirationDateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
				.format(expiryTimeNoFractionalSeconds.plus(2, ChronoUnit.HOURS));

		data.addProperty("ExpirationDateTime", expirationDateTime);

		JsonObject o = new JsonObject();
		o.add("Data", data);
		o.add("Risk", new JsonObject());

		env.putObject("account_requests_endpoint_request", o);

		logSuccess(args("account_requests_endpoint_request", o));

		return env;
	}

}
