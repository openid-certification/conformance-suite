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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.as.ExtractClientCertificateFromRequestHeaders;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ExtractClientCertificateFromRequestHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject sampleHeaders;

	private ExtractClientCertificateFromRequestHeaders cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ExtractClientCertificateFromRequestHeaders("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Example from RFC 7468
		String certificate = "-----BEGIN CERTIFICATE----- " + 
				"MIIBmTCCAUegAwIBAgIBKjAJBgUrDgMCHQUAMBMxETAPBgNVBAMTCEF0bGFudGlz " + 
				"MB4XDTEyMDcwOTAzMTAzOFoXDTEzMDcwOTAzMTAzN1owEzERMA8GA1UEAxMIQXRs " + 
				"YW50aXMwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAu+BXo+miabDIHHx+yquqzqNh " + 
				"Ryn/XtkJIIHVcYtHvIX+S1x5ErgMoHehycpoxbErZmVR4GCq1S2diNmRFZCRtQID " + 
				"AQABo4GJMIGGMAwGA1UdEwEB/wQCMAAwIAYDVR0EAQH/BBYwFDAOMAwGCisGAQQB " + 
				"gjcCARUDAgeAMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDAzA1BgNVHQEE " + 
				"LjAsgBA0jOnSSuIHYmnVryHAdywMoRUwEzERMA8GA1UEAxMIQXRsYW50aXOCASow " + 
				"CQYFKw4DAh0FAANBAKi6HRBaNEL5R0n56nvfclQNaXiDT174uf+lojzA4lhVInc0 " + 
				"ILwpnZ1izL4MlI9eCSHhVQBHEp2uQdXJB+d5Byg= " + 
				"-----END CERTIFICATE-----";

		sampleHeaders = new JsonObject();
		sampleHeaders.addProperty("X-Ssl-Cert", certificate);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.ExtractClientCertificateFromRequestHeaders#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.put("client_request_headers", sampleHeaders);

		cond.evaluate(env);

		assertThat(env.containsObj("client_certificate")).isTrue();
		assertThat(env.getString("client_certificate", "subject.dn")).isEqualTo("CN=Atlantis");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.ExtractClientCertificateFromRequestHeaders#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.evaluate(env);

	}

}
