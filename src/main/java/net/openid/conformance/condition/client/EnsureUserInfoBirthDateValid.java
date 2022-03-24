package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Calendar;
import java.util.Locale;

public class EnsureUserInfoBirthDateValid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env) {

		String birthDate = env.getString("userinfo", "birthdate");

		if (birthDate == null) {
			log("userinfo response does not contain 'birthdate'");
		} else {

			if (!isValidBirthDate(birthDate)) {
				throw error("Format of 'birthdate' in userinfo response is invalid", args("birthdate", birthDate));
			}

			logSuccess("Format of 'birthdate' in userinfo response is valid", args("birthdate", birthDate));
		}

		return env;
	}

	public static boolean isValidBirthDate(String date) {
		return isValidFullDate(date) || isValidYearOnly(date);
	}

	private static boolean isValidFullDate(String date) {
		// US used as per https://developer.android.com/reference/java/util/Locale.html#be-wary-of-the-default-locale
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US);
		try {
			LocalDate parsedDate = LocalDate.parse(date, dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT));
			if (parsedDate.getYear() == 0) {
				// as per OIDCC, the year can optionally be 0000 to indicate year not held/not released
				return true;
			}
			int year = parsedDate.getYear();
			if (!isSaneBirthYear(year)) {
				return false;
			}

			return true;

		} catch (DateTimeParseException e) {
			return false;
		}
	}

	// true if seems like a real date of birth, or at least a fake that results in a non-negative non-excessive age.
	private static boolean isSaneBirthYear(int year) {
		return year >= 1850 && year <= Calendar.getInstance().get(Calendar.YEAR);
	}

	private static boolean isValidYearOnly(String yearStr) {
		try {
			int year = Integer.parseInt(yearStr);
			return isSaneBirthYear(year);
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
