package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

public class CheckForDateHeaderInResourceResponse extends AbstractCondition {

	private static final TemporalAmount DATE_TOLERANCE = Duration.ofMinutes(5);

	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers")
	@SuppressWarnings("JavaTimeDefaultTimeZone")
	public Environment evaluate(Environment env) {

		String dateStr = env.getString("resource_endpoint_response_headers", "date");

		if (Strings.isNullOrEmpty(dateStr)) {
			throw error("Date header not found in resource endpoint response");
		}

		try {

			ZonedDateTime parsedDate = ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME);

			ZonedDateTime now = ZonedDateTime.now();

			if (now.minus(DATE_TOLERANCE).isBefore(parsedDate)
				&& now.plus(DATE_TOLERANCE).isAfter(parsedDate)) {

				logSuccess("Date header present and validated", args("date", dateStr, "skew", ChronoUnit.MILLIS.between(parsedDate, now)));

				return env;
			} else {
				throw error("Excessive difference from current time", args("date", dateStr, "skew", ChronoUnit.MILLIS.between(parsedDate, now)));
			}

		} catch (DateTimeParseException e) {
			throw error("Invalid date format", e, args("date", dateStr));
		}
	}

}
