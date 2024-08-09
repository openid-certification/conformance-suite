package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractClientCertificateFromRequestHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject tokenEndpointRequest;

	private ExtractClientCertificateFromRequestHeaders cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractClientCertificateFromRequestHeaders();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

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
	 * Test method for {@link ExtractClientCertificateFromRequestHeaders#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_request", tokenEndpointRequest);

		cond.execute(env);

		assertThat(env.containsObject("client_certificate")).isTrue();
		assertThat(env.getString("client_certificate", "subject.dn")).isEqualTo("CN=Atlantis");

	}

	/**
	 * Test method for {@link ExtractClientCertificateFromRequestHeaders#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			tokenEndpointRequest.add("headers", new JsonObject());

			env.putObject("token_endpoint_request", tokenEndpointRequest);

			cond.execute(env);

		});

	}

}
