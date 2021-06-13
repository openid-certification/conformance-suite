package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ExtractClientMTLSCertificateSubject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractClientMTLSCertificateSubject cond;

	private String cert =
			"MIIGRTCCBS2gAwIBAgIURAKCAjYgqEjx+Ss4WTOsrUh69EcwDQYJKoZIhvcNAQEL" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIxMDUxMDE4MzcwMFoXDTIyMDYxMDA0" +
			"MzcwMFowgYIxCzAJBgNVBAYTAkJSMRUwEwYDVQQKEwxPcGVuIEJhbmtpbmcxLTAr" +
			"BgNVBAsTJDc0ZTkyOWQ5LTMzYjYtNGQ4NS04YmE3LWMxNDZjODY3YTgxNzEtMCsG" +
			"A1UEAxMkYjg5ODZhNDMtZTczMC00NjBmLTk3ZjktOWFkOTBmNGUzMWQzMIIBIjAN" +
			"BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtQqkGwOmNTsVgmMl4L0PHMoPo1hK" +
			"+eDVtNOI6maM5CVYUaVPvSBMioSOhp8HwDHDwclao3Uy0A8est058fxKA0vJ9dUv" +
			"OfDYw3xLoMacWuEMZQbCBeM7wjOxwWapduieA29tqGDUb4/Zo1ssAmfbtSyRIzOc" +
			"7xdKYpowN0uktNeLicewJbDeXt5YO+CKARM8YhzBMTeiZFIftu++GExIK3GOEWpN" +
			"5BJyIX6eu/MTQYJhdclQI3mWrSRWD56UjOpGt27SCzzMlMIxZU/0hexYbAzMCh2X" +
			"IczxM8XAbdTMTTBw92H3lpWzDDl7xCW8UJezstANizskcLJP22mKSgzmPQIDAQAB" +
			"o4ICwTCCAr0wDgYDVR0PAQH/BAQDAgOoMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggr" +
			"BgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBQpjnRrRowwwsHxMvDE0EIP" +
			"HTeMhTAfBgNVHSMEGDAWgBSGf1itF/WCtk60BbP7sM4RQ99MvjBMBggrBgEFBQcB" +
			"AQRAMD4wPAYIKwYBBQUHMAGGMGh0dHA6Ly9vY3NwLnNhbmRib3gucGtpLm9wZW5i" +
			"YW5raW5nYnJhc2lsLm9yZy5icjBLBgNVHR8ERDBCMECgPqA8hjpodHRwOi8vY3Js" +
			"LnNhbmRib3gucGtpLm9wZW5iYW5raW5nYnJhc2lsLm9yZy5ici9pc3N1ZXIuY3Js" +
			"MIIBoQYDVR0gBIIBmDCCAZQwggGQBgorBgEEAYO6L2QBMIIBgDCCATYGCCsGAQUF" +
			"BwICMIIBKAyCASRUaGlzIENlcnRpZmljYXRlIGlzIHNvbGVseSBmb3IgdXNlIHdp" +
			"dGggUmFpZGlhbSBTZXJ2aWNlcyBMaW1pdGVkIGFuZCBvdGhlciBwYXJ0aWNpcGF0" +
			"aW5nIG9yZ2FuaXNhdGlvbnMgdXNpbmcgUmFpZGlhbSBTZXJ2aWNlcyBMaW1pdGVk" +
			"cyBUcnVzdCBGcmFtZXdvcmsgU2VydmljZXMuIEl0cyByZWNlaXB0LCBwb3NzZXNz" +
			"aW9uIG9yIHVzZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBSYWlkaWFt" +
			"IFNlcnZpY2VzIEx0ZCBDZXJ0aWZpY2F0ZSBQb2xpY3kgYW5kIHJlbGF0ZWQgZG9j" +
			"dW1lbnRzIHRoZXJlaW4uMEQGCCsGAQUFBwIBFjhodHRwOi8vY3BzLnNhbmRib3gu" +
			"cGtpLm9wZW5iYW5raW5nYnJhc2lsLm9yZy5ici9wb2xpY2llczANBgkqhkiG9w0B" +
			"AQsFAAOCAQEAReGyXrlqOx7YlriIeIIFQVLvJHndAVvYkyjj08fO+kJxzpmLheH0" +
			"fZOXYakK/H0RoPqxaeoumy5WeOui+8vD1vaRS20gDRtOVxfMv80khp4kM/JQS4Q5" +
			"SFP5yzHAs3kB3efdUIMg8XzjKm+11uCSxW3WCK+nS3zJvJbpFMKCcV4wJgixn2pC" +
			"QS+Vea80KnDybFVBGCfhf5LLgqW83h1RBqRw+O7SgcuMpwk30+ZMpE7JuN1cHT+1" +
			"K1Nro7wlYqlyCm2GiyaUI2UgF9pv0Y3ao+91p/tAV341iEJZcg0r7+s6sTdRMf2V" +
			"/z6+viXbPU8muS++t4RX+LqJZfCVM7HsnQ==";

	@Before
	public void setUp() throws Exception {

		Security.addProvider(new BouncyCastleProvider());

		cond = new ExtractClientMTLSCertificateSubject();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_noError() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"cert\":\"" + cert + "\""
			+ "}").getAsJsonObject();

		env.putObject("mutual_tls_authentication", config);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("mutual_tls_authentication", "cert");

		assertThat(env.getString("certificate_subject", "subjectdn")).isEqualTo("CN=b8986a43-e730-460f-97f9-9ad90f4e31d3,OU=74e929d9-33b6-4d85-8ba7-c146c867a817,O=Open Banking,C=BR");
		assertThat(env.getString("certificate_subject", "ou")).isEqualTo("74e929d9-33b6-4d85-8ba7-c146c867a817");
		assertThat(env.getString("certificate_subject", "cn")).isEqualTo("b8986a43-e730-460f-97f9-9ad90f4e31d3");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {
		cond.execute(env);
	}

}
