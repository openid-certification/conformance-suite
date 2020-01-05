package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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

	private boolean isValidBirthDate(String date) {

		if (isValidDateWithFormat(date, "yyyy-MM-dd")
			|| isValidDateWithFormat(date, "yyyy")
			|| isValidDateWithFormat(date, "0000-MM-dd")) {
			return  true;
		}
		return false;
	}

	private boolean isValidDateWithFormat(String value, String dateFormat) {
		DateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.setLenient(false);
		try {
			sdf.parse(value);
		} catch (ParseException e) {
			return false;
		}
		return true;
	}

}
