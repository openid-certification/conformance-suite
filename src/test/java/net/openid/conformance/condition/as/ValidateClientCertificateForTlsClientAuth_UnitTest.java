package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateClientCertificateForTlsClientAuth_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateClientCertificateForTlsClientAuth cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateClientCertificateForTlsClientAuth();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		JsonObject sampleHeaders = new JsonObject();
		String certificate = """
				-----BEGIN CERTIFICATE-----
				MIIGCDCCA/CgAwIBAgIJAIgwloUBq+0LMA0GCSqGSIb3DQEBCwUAMGYxCzAJBgNV
				BAYTAlVTMQswCQYDVQQIDAJDQTESMBAGA1UEBwwJU2FuIFJhbW9uMQ0wCwYDVQQK
				DARPSURGMScwJQYDVQQDDB50ZXN0LmNlcnRpZmljYXRpb24uZXhhbXBsZS5jb20w
				HhcNMjAwNTIxMDYzNTAyWhcNMzAwNTE5MDYzNTAyWjBmMQswCQYDVQQGEwJVUzEL
				MAkGA1UECAwCQ0ExEjAQBgNVBAcMCVNhbiBSYW1vbjENMAsGA1UECgwET0lERjEn
				MCUGA1UEAwwedGVzdC5jZXJ0aWZpY2F0aW9uLmV4YW1wbGUuY29tMIICIjANBgkq
				hkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAwFCUwv8D5ReG/bXSiHSuLQ0laRG+paST
				PlXLhflpGh4pWsHoPaLHmIwScXKDYZVUGtvLzQkkFT6QODn3FXWogxkzNCLpbpvk
				iiHK/1NdibYeKaWj0CnaFj6v5ObpsMo5QpQkCPlOPaARGjnsNoEVBhqFdbp4xqiw
				6ZmBBNYt8iZ8D9ta50UJmwYPyOiwyacKni5XBjfbjq7M4pubHSg1fgzDxlJdSEKa
				n9nNMTLRXor3qvJLUpbDR4x+NjKfezlpeKI3Y0GyEdB0DxOYJSOgF8wq6gSMTr7F
				SAVacXRKIxyzAICmFZj4wYGQ2wYhQXB+wgbMw9zmD25NlwqTHQLcN9fi2Ew/Abvu
				J79OoMDLZrbInXKtPObv0Szz7A2tBv4O6juQRlkTfs27z1K1hFr+MKzXNd4vgGVv
				PUIdluKZzxx5qGcg/ya07LRl4qn06zm+ectdCeKUuFQi3q0YVE3NSRXgv2VTpzWb
				5zy4iJlxifiInp3miiYP9kfRKR7DBLWV6/RsrObmgJUeUjiwFEfimaeM5NFKYwdE
				TLhAB/1L28EevM5k6jdRfDHsqGbEL5T630G6iwu1Ih+N7GNVobkJuzOYPtwyxRwN
				8vcVVCvQ2k41sNbpnacg8SyRBepeiIQR+xC1avvfF148tUwnhz1vQqR4Cx3Pbrsh
				+V21WCWsH9MCAwEAAaOBuDCBtTCBsgYDVR0RBIGqMIGngg93d3cuZXhhbXBsZS5j
				b22CIGRuc25hbWUyLmNvbmZvcm1hbmNlLmV4YW1wbGUuY29thwR/AAABhwQKAAAB
				hiRodHRwczovL3d3dy5jZXJ0aWZpY2F0aW9uLm9wZW5pZC5uZXSGEWh0dHBzOi8v
				bG9jYWxob3N0gRhjZXJ0aWZpY2F0aW9uQG9wZW5pZC5uZXSBE2pvaG5kb2VAZXhh
				bXBsZS5jb20wDQYJKoZIhvcNAQELBQADggIBAENIqUWJgwFq9eXWkM3yWZ1p2HqV
				e0dAwCBPMT1wUQ8OdzPgR9AzZxAhv3uqHmCqEY8eeFXQyMgz9lNPjTvnzVQxAFH4
				SqH20S3mh/ymMSMaZsHb8/acziXtY6qtTpwwjJmp9szx+fXlMrssr51HAivbI1ea
				PI2PzpwgHJIlthg5DSbvoYhNuvUtv33N9zzOcFTBLcGcdLXeVisnCXMmltweyUM3
				AKqT9eMWZfxCMg69eFPNs9nvQ1u9BQqPYns2illfFdtL4hN6S6v4WjUiUS2IEmmJ
				h8k9xLHwb8ZQucdOb3V4ybHGqx7/aigHOcvpbUL+4aAuzyVtU20QR3wQXJ9dlRyk
				kpz+RJLH2h45JaWtS4T0dv+NmATXjIcpEqRMDRpZT4y35wkMfX375CBaV0OAm+0T
				1DvD9NFa2HQUQTV/vedJIXavF0yswuaPyY8sKpH4v66FpZJYpO8K1O+JGM3/sGgE
				X3LhcekIOnSZzwBraRian7u1fJhfSmAUlBcnaxtMpZ2XVGvtedz+NA9lkXvD9RI9
				u/9XJsTKaQn35FbGG9W18bm9JkB9IU08hekygqSJK9v/6ajD6S1U5SBgrQ7GfQya
				ygd6HbUDO51D2El4vqxBcbMlzPEItj11b0xaBM1ZuF64/s3BzzwRyuyTOU5gu4SL
				PjBLfDw2pK+NfvAb
				-----END CERTIFICATE-----
				""";
		sampleHeaders.addProperty("x-ssl-cert", certificate);
		JsonObject tokenEndpointRequest = new JsonObject();
		tokenEndpointRequest.add("headers", sampleHeaders);
		env.putObject("token_endpoint_request", tokenEndpointRequest);

		ExtractClientCertificateFromRequestHeaders extractCond = new ExtractClientCertificateFromRequestHeaders();
		extractCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		extractCond.execute(env);

	}

	@Test
	public void evaluate_SubjectDN() {
		JsonObject client = new JsonObject();
		client.addProperty("tls_client_auth_subject_dn", "cn=test.certification.example.com,o=oidf,l=san ramon,st=ca,c=us");
		env.putObject("client", client);
		cond.evaluate(env);
	}

	@Test
	public void evaluate_SubjectDN_Reordered() {
		JsonObject client = new JsonObject();
		client.addProperty("tls_client_auth_subject_dn", "c=us,st=ca,l=san ramon,o=oidf,cn=test.certification.example.com");
		env.putObject("client", client);
		cond.evaluate(env);
	}

	@Test
	public void evaluate_Error() {
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			client.addProperty("tls_client_auth_subject_dn", "cn=INVALIDtest.certification.example.com,o=oidf,l=san ramon,st=ca,c=us");
			env.putObject("client", client);
			cond.evaluate(env);
		});
	}

	@Test
	public void evaluate_Multiple() {
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			client.addProperty("tls_client_auth_subject_dn", "cn=test.certification.example.com,o=oidf,l=san ramon,st=ca,c=us");
			client.addProperty("tls_client_auth_san_dns", "dnsname2.conformance.example.com");
			env.putObject("client", client);
			cond.evaluate(env);
		});
	}

	@Test
	public void evaluate_DnsName() {
		JsonObject client = new JsonObject();
		client.addProperty("tls_client_auth_san_dns", "dnsname2.conformance.example.com");
		env.putObject("client", client);
		cond.evaluate(env);
	}

	@Test
	public void evaluate_Uri() {
		JsonObject client = new JsonObject();
		client.addProperty("tls_client_auth_san_uri", "https://www.certification.openid.net");
		env.putObject("client", client);
		cond.evaluate(env);
	}

	@Test
	public void evaluate_IP() {
		JsonObject client = new JsonObject();
		client.addProperty("tls_client_auth_san_ip", "10.0.0.1");
		env.putObject("client", client);
		cond.evaluate(env);
	}

	@Test
	public void evaluate_Email() {
		JsonObject client = new JsonObject();
		client.addProperty("tls_client_auth_san_email", "certification@openid.net");
		env.putObject("client", client);
		cond.evaluate(env);
	}

	@Test
	public void evaluate_EmailUpperCase() {
		JsonObject client = new JsonObject();
		client.addProperty("tls_client_auth_san_email", "certification@openid.NET");
		env.putObject("client", client);
		cond.evaluate(env);
	}

	@Test
	public void checkSanIP_IPv6Shortened() {
		JsonArray values = new JsonArray();
		values.add("127.0.0.1");
		values.add("0:0:0:0:0:0:0:1");
		values.add("2001:db8:0:0:1:0:0:1");
		assertTrue(cond.checkSanIP(values, "2001:db8::1:0:0:1"));
	}

	@Test
	public void checkSanIP_IPv6Localhost() {
		JsonArray values = new JsonArray();
		values.add("127.0.0.1");
		values.add("0:0:0:0:0:0:0:1");
		values.add("2001:db8:0:0:1:0:0:1");
		assertTrue(cond.checkSanIP(values, "::1"));
	}

	@Test
	public void checkSanIP_IPv6Uppercase() {
		JsonArray values = new JsonArray();
		values.add("127.0.0.1");
		values.add("0:0:0:0:0:0:0:1");
		//D is uppercase |
		values.add("2001:Db8:0:0:1:0:0:1");
		assertTrue(cond.checkSanIP(values, "2001:db8::1:0:0:1"));
	}

	@Test
	public void checkSanURI_Uppercase() {
		JsonArray values = new JsonArray();
		values.add("HTTPS://example.com/abc");
		values.add("https://Example.com/bcd");
		assertTrue(cond.checkSanUri(values, "https://example.com/abc"));
		assertTrue(cond.checkSanUri(values, "https://example.com/bcd"));
	}
}
