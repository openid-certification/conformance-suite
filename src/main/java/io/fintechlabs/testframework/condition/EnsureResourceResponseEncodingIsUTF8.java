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

import java.nio.charset.Charset;

import org.springframework.http.InvalidMediaTypeException;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureResourceResponseEncodingIsUTF8 extends AbstractCondition {

	public EnsureResourceResponseEncodingIsUTF8(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String contentTypeStr = env.getString("resource_endpoint_response_headers", "Content-Type");

		if (!Strings.isNullOrEmpty(contentTypeStr)) {
			try {
				MediaType parsedType = MediaType.parse(contentTypeStr);
				Optional<Charset> charset = parsedType.charset();
				if (charset.isPresent()) {
					String charsetName = charset.get().name();
					if (charsetName.equals("UTF-8")) {
						logSuccess("Response charset is UTF-8", args("content_type", contentTypeStr));
						return env;
					} else {
						return error("Response charset is not UTF-8",
								args("content_type", contentTypeStr,
										"charset", charset.get().name()));
					}
				}
			} catch (InvalidMediaTypeException e) {
				return error("Unable to parse content type", args("content_type", contentTypeStr));
			}
		}

		logSuccess("No charset was declared for response; parsed as UTF-8", args("content_type", Strings.nullToEmpty(contentTypeStr)));
		return env;

	}

}
