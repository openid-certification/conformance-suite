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

package io.fintechlabs.testframework.condition.as;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractClientCertificateFromTokenEndpointRequestHeaders extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public ExtractClientCertificateFromTokenEndpointRequestHeaders(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = "client_certificate")
	public Environment evaluate(Environment env) {

		// Remove any certificate from a previous connection
		env.remove("client_certificate");

		String certStr = env.getString("token_endpoint_request", "headers.X-Ssl-Cert");
		if (certStr == null) {
			throw error("Client certificate not found");
		}

		try {

			String certPem = certStr.replaceAll("\\s+(?!CERTIFICATE-----)", "\n");

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certPem.getBytes()));

			JsonObject certInfo = new JsonObject();

			JsonObject subjectInfo = new JsonObject();
			X500Principal subject = cert.getSubjectX500Principal();
			subjectInfo.addProperty("dn", subject.getName());
			certInfo.add("subject", subjectInfo);

			env.put("client_certificate", certInfo);

			logSuccess("Extracted client certificate", args("client_certificate", certInfo));

			return env;

		} catch (CertificateException e) {
			throw error("Error parsing certificate", e, args("cert", certStr));
		}

	}

}
