/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.condition.client;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForDateHeaderInResourceResponse extends AbstractCondition {

	private static final String PATTERN_IMF_FIXDATE = "EEE, dd MMM yyyy HH:mm:ss zzz";

	private static final String GMT = "GMT";

	private static final long DATE_TOLERANCE_MS = 5 * 60 * 1000; // 5 minutes

	public CheckForDateHeaderInResourceResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String dateStr = env.getString("resource_endpoint_response_headers", "Date");

		if (Strings.isNullOrEmpty(dateStr)) {
			throw error("Date header not found in resource endpoint response");
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat(PATTERN_IMF_FIXDATE, Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone(GMT));

		ParsePosition pos = new ParsePosition(0);
		Date messageDate = dateFormat.parse(dateStr, pos);

		if (messageDate == null) {
			// null means that the date could not be parsed
			throw error("Invalid date format", args("date", dateStr));
		} else if (pos.getIndex() < dateStr.length()) {
			throw error("Trailing characters in date", args("date", dateStr));
		} else if (!dateStr.endsWith(GMT)) {
			throw error("Non-GMT timezone", args("date", dateStr));
		}

		long now = System.currentTimeMillis();

		long skew = messageDate.getTime() - now;

		if (Math.abs(skew) > DATE_TOLERANCE_MS) {
			throw error("Excessive difference from current time", args("date", dateStr, "skew_ms", skew));
		}

		logSuccess("Date header present and validated", args("date", dateStr, "skew_ms", skew));

		return env;
	}

}
