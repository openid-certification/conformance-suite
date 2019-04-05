package io.fintechlabs.testframework.condition.rs;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractFapiDateHeader extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractFapiDateHeader(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
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
