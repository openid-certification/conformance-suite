package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ExtractFapiDateHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "fapi_auth_date")
	public Environment evaluate(Environment env) {

		String header = env.getString("incoming_request", "headers.x-fapi-auth-date");
		if (Strings.isNullOrEmpty(header)) {
			throw error("Couldn't find FAPI auth date header");
		} else {

			try {
				// try to parse it to make sure it's in the right format
				@SuppressWarnings("unused")
				ZonedDateTime parsedDate = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME);

				env.putString("fapi_auth_date", header);
				logSuccess("Found a FAPI auth date header", args("fapi_auth_date", header));

				return env;
			} catch (DateTimeParseException e) {
				throw error("Could not parse FAPI auth date header", e, args("fapi_auth_date", header));
			}
		}

	}

}
