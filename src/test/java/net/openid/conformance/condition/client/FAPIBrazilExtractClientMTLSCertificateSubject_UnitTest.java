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
public class FAPIBrazilExtractClientMTLSCertificateSubject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIBrazilExtractClientMTLSCertificateSubject cond;

	@Before
	public void setUp() throws Exception {

		Security.addProvider(new BouncyCastleProvider());

		cond = new FAPIBrazilExtractClientMTLSCertificateSubject();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_oldStyleCertError() {
		String cert =
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

		JsonObject config = new JsonParser().parse("{"
			+ "\"cert\":\"" + cert + "\""
			+ "}").getAsJsonObject();

		env.putObject("mutual_tls_authentication", config);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("mutual_tls_authentication", "cert");

		assertThat(env.getString("certificate_subject", "subjectdn")).isEqualTo("CN=b8986a43-e730-460f-97f9-9ad90f4e31d3,OU=74e929d9-33b6-4d85-8ba7-c146c867a817,O=Open Banking,C=BR");
		assertThat(env.getString("certificate_subject", "ou")).isEqualTo("74e929d9-33b6-4d85-8ba7-c146c867a817");
		assertThat(env.getString("certificate_subject", "brazil_software_id")).isEqualTo("b8986a43-e730-460f-97f9-9ad90f4e31d3");
	}

	@Test
	public void testEvaluate_newStyleCertNoError() {

		// Brazil has certificates with the DN in two different formats; this is the second format
		String altCert =
				"MIIG/TCCBeWgAwIBAgIUKr/974UGyb+UuCP3AhUxo4vV5lwwDQYJKoZIhvcNAQEL" +
				"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx" +
				"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB" +
				"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIxMDYxNjIwMDAwMFoXDTIyMDcxNjIw" +
				"MDAwMFowggEqMQswCQYDVQQGEwJCUjEmMCQGA1UECBMdQk9UQUZPR08gLyBSSU8g" +
				"REUgSkFORUlSTywgUkoxETAPBgNVBAcTCEJPVEFGT0dPMRwwGgYDVQQKExNPcGVu" +
				"IEJhbmtpbmcgQnJhc2lsMS0wKwYDVQQLEyQ3NGU5MjlkOS0zM2I2LTRkODUtOGJh" +
				"Ny1jMTQ2Yzg2N2E4MTcxETAPBgNVBAMTCFdoYXRldmVyMRYwFAYDVQQFEw0xMzM1" +
				"MzIzNjAwMTg5MTQwMgYKCZImiZPyLGQBARMkYmU0MmQzYTUtOTg2MS00MzA1LTlk" +
				"ZWItYTIyNzU2ZWE3ZTgyMR0wGwYDVQQPExRQcml2YXRlIE9yZ2FuaXphdGlvbjET" +
				"MBEGCysGAQQBgjc8AgEDEwJCUjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC" +
				"ggEBANXyk4DfVmIYvIv4DNsSjOO7icHCxhNFrOSfrgQNbRVFOpvvdY9n+udntoWx" +
				"J3vky3rZKxcKZadu3XuwuGiDmC1YwBRAzfZwe2ncpHIUENfUIQs/kgtz0IUtu3yx" +
				"jx2wFYb3s879vHmYc9nAish6Y7HEnKbMR1dqif7zhAzb2XztfbCqzRimMafUQ6bU" +
				"M6DdELVMpc7gs/2q5oQ8C0GZXHrHFO7xxcx7ySF6CMoDbonmgTwH3MWov4Xd67Eq" +
				"VkSZN+aBJI1ksPwl83VGMt30ji1G/wpxCX+Ov5Yyi8BzDRd08PBAmZuaPrVPcjXe" +
				"adYIz/DobnI/iJEJR1p5h2sfI4UCAwEAAaOCAtAwggLMMAwGA1UdEwEB/wQCMAAw" +
				"HwYDVR0jBBgwFoAUhn9YrRf1grZOtAWz+7DOEUPfTL4wTAYIKwYBBQUHAQEEQDA+" +
				"MDwGCCsGAQUFBzABhjBodHRwOi8vb2NzcC5zYW5kYm94LnBraS5vcGVuYmFua2lu" +
				"Z2JyYXNpbC5vcmcuYnIwSwYDVR0fBEQwQjBAoD6gPIY6aHR0cDovL2NybC5zYW5k" +
				"Ym94LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIvaXNzdWVyLmNybDAOBgNV" +
				"HQ8BAf8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFOgyoKUM" +
				"svZWgXFGO3QNmddqLrRTMBcGA1UdEQQQMA6CDGFueXRoaW5nLmNvbTCCAaEGA1Ud" +
				"IASCAZgwggGUMIIBkAYKKwYBBAGDui9kATCCAYAwggE2BggrBgEFBQcCAjCCASgM" +
				"ggEkVGhpcyBDZXJ0aWZpY2F0ZSBpcyBzb2xlbHkgZm9yIHVzZSB3aXRoIFJhaWRp" +
				"YW0gU2VydmljZXMgTGltaXRlZCBhbmQgb3RoZXIgcGFydGljaXBhdGluZyBvcmdh" +
				"bmlzYXRpb25zIHVzaW5nIFJhaWRpYW0gU2VydmljZXMgTGltaXRlZHMgVHJ1c3Qg" +
				"RnJhbWV3b3JrIFNlcnZpY2VzLiBJdHMgcmVjZWlwdCwgcG9zc2Vzc2lvbiBvciB1" +
				"c2UgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgUmFpZGlhbSBTZXJ2aWNl" +
				"cyBMdGQgQ2VydGljaWNhdGUgUG9saWN5IGFuZCByZWxhdGVkIGRvY3VtZW50cyB0" +
				"aGVyZWluLjBEBggrBgEFBQcCARY4aHR0cDovL2Nwcy5zYW5kYm94LnBraS5vcGVu" +
				"YmFua2luZ2JyYXNpbC5vcmcuYnIvcG9saWNpZXMwDQYJKoZIhvcNAQELBQADggEB" +
				"AMVVUTONB0BPZisjIyDjJq3k5WTSdWy3Ugbkbr0zk40E6B0CxkD9ZS/UY3JE0rlq" +
				"kaYfCqe29lEU6jZkqOq3E57S0irkKZYvntt6ueRcj+kA/MkfLuqn1PktkV4uY84B" +
				"XWCQljuYKH03irEa84OD92q0YSsb0NjAXHc1Mk+En5lC1A8KVrS5x5sMEL1+m7XO" +
				"Y5m8f/2p++RawWQ2uEZD6qliRLCJSaxeY+S63b7A9deA9XkPIIK7NtTumSYIZvnJ" +
				"HSF7jldwtlW3Fy9alQEI5yNhkrGIHwz/EnBLjmZhqR/mrA5K8IdoyO9Q6tIJ2YZ/" +
				"yNQ50KwYtTAV23CIMl5bOYQ=";

		JsonObject config = new JsonParser().parse("{"
			+ "\"cert\":\"" + altCert + "\""
			+ "}").getAsJsonObject();

		env.putObject("mutual_tls_authentication", config);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("mutual_tls_authentication", "cert");

		// note that subjectdn includes a number of numeric oids - see comment in FAPIBrazilExtractClientMTLSCertificateSubject.java
		assertThat(env.getString("certificate_subject", "subjectdn")).isEqualTo("1.3.6.1.4.1.311.60.2.1.3=#13024252,2.5.4.15=#131450726976617465204f7267616e697a6174696f6e,UID=be42d3a5-9861-4305-9deb-a22756ea7e82,2.5.4.5=#130d31333335333233363030313839,CN=Whatever,OU=74e929d9-33b6-4d85-8ba7-c146c867a817,O=Open Banking Brasil,L=BOTAFOGO,ST=BOTAFOGO / RIO DE JANEIRO\\, RJ,C=BR");
		assertThat(env.getString("certificate_subject", "ou")).isEqualTo("74e929d9-33b6-4d85-8ba7-c146c867a817");
		assertThat(env.getString("certificate_subject", "brazil_software_id")).isEqualTo("be42d3a5-9861-4305-9deb-a22756ea7e82");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {
		cond.execute(env);
	}

}
