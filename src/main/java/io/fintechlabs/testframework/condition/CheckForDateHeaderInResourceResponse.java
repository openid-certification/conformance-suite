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

package io.fintechlabs.testframework.condition;

import java.util.Date;

import org.apache.http.client.utils.DateUtils;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForDateHeaderInResourceResponse extends AbstractCondition {

	private static final long DATE_TOLERANCE_MS = 5 * 60 * 1000; // 5 minutes

	public CheckForDateHeaderInResourceResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String dateStr = env.getString("resource_endpoint_response_headers", "Date");

		if (Strings.isNullOrEmpty(dateStr)) {
			return error("Date header not found in resource endpoint response");
		}

		Date messageDate = DateUtils.parseDate(dateStr);

		if (messageDate == null) {
			// null means that the date could not be parsed
			return error("Invalid date format", args("date", dateStr));
		}

		long now = System.currentTimeMillis();

		long skew = messageDate.getTime() - now;

		if (Math.abs(skew) > DATE_TOLERANCE_MS) {
			return error("Excessive difference from current time", args("date", dateStr, "skew_ms", skew));
		}

		logSuccess("Date header present and validated", args("date", dateStr, "skew_ms", skew));

		return env;
	}

}
