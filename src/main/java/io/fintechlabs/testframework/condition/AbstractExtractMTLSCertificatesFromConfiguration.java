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

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public abstract class AbstractExtractMTLSCertificatesFromConfiguration extends AbstractCondition {

	private static final Pattern PEM_PATTERN = Pattern.compile("^-----BEGIN [^-]+-----$(.*?)^-----END [^-]+-----$", Pattern.MULTILINE | Pattern.DOTALL);

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public AbstractExtractMTLSCertificatesFromConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	protected String stripPEM(String in) throws IllegalArgumentException {

		Matcher m = PEM_PATTERN.matcher(in);

		if (m.find()) {

			// There may be multiple certificates, so concatenate and re-encode
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			do {
				String certStr = m.group(1).replaceAll("[\r\n]", "");
				byte[] cert = Base64.getDecoder().decode(certStr);
				out.write(cert, 0, cert.length);
			} while (m.find());

			return Base64.getEncoder().encodeToString(out.toByteArray());
		} else {

			// Assume it's a Base64-encoded DER format; check that it decodes OK
			Base64.getDecoder().decode(in);
			return in;
		}

	}

}
