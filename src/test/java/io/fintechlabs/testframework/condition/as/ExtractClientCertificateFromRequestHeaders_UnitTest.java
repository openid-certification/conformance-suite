package io.fintechlabs.testframework.condition.as;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ExtractClientCertificateFromRequestHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject tokenEndpointRequest;

	private ExtractClientCertificateFromTokenEndpointRequestHeaders cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ExtractClientCertificateFromTokenEndpointRequestHeaders("UNIT-TEST", eventLog, ConditionResult.INFO);

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

		JsonObject sampleHeaders = new JsonObject();
		sampleHeaders.addProperty("x-ssl-cert", certificate);

		tokenEndpointRequest = new JsonObject();
		tokenEndpointRequest.add("headers", sampleHeaders);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.ExtractClientCertificateFromTokenEndpointRequestHeaders#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_request", tokenEndpointRequest);

		cond.evaluate(env);

		assertThat(env.containsObject("client_certificate")).isTrue();
		assertThat(env.getString("client_certificate", "subject.dn")).isEqualTo("CN=Atlantis");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.ExtractClientCertificateFromTokenEndpointRequestHeaders#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.evaluate(env);

	}

}
