package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestModule.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * {@link ValidateDiscoverySignedMetadata} is an {@link net.openid.conformance.sequence.AbstractConditionSequence},
 * so it cannot be exercised through {@code Condition.execute(env)}; it has to run through a test module that calls
 * {@code call(sequence(...))}. This test drives the sequence end-to-end via a minimal {@link AbstractTestModule}
 * harness and asserts on the resulting test {@link Result}.
 *
 * <p>Two failure shapes are possible:
 * <ul>
 *   <li>stop-on-failure sub-conditions ({@link ExtractPDPSignedMetadata}, {@link VerifyAuthzenSignedMetadataSignature})
 *       throw a {@link TestFailureException};</li>
 *   <li>continue-on-failure sub-conditions (alg / iss / iat / exp / nbf / nested signed_metadata) leave the module
 *       {@link Result} at {@code FAILED} or {@code WARNING} without throwing.</li>
 * </ul>
 *
 * <p>The sequence is only invoked by {@link net.openid.conformance.authzen.AbstractAuthzenPDPTest} when
 * {@code signed_metadata} is actually present, so there is no "absent signed_metadata" case here — every scenario
 * supplies a {@code signed_metadata} value.
 */
class ValidateDiscoverySignedMetadata_UnitTest {

	// 256-bit secret for HS256.
	private static final byte[] HMAC_SECRET = "0123456789abcdef0123456789abcdef".getBytes();

	private static final String PDP_ISSUER = "https://pdp.example.com";

	private Harness module;

	@BeforeEach
	public void setUp() {
		module = new Harness();
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		TestInfoService infoService = mock(TestInfoService.class);
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test"), eventLog, null, infoService, null, null);
		// config carries the trusted PDP identifier; no 'pdp.jwks' is configured, so the signature verification
		// sub-condition is skipped and the structural checks (alg / iss / claims) are what's under test here.
		JsonObject config = new JsonObject();
		JsonObject pdpCfg = new JsonObject();
		pdpCfg.addProperty("policy_decision_point", PDP_ISSUER);
		config.add("pdp", pdpCfg);
		module.putObject("config", config);
	}

	private void putSignedMetadata(String token) {
		JsonObject pdp = new JsonObject();
		if (token != null) {
			pdp.addProperty("signed_metadata", token);
		}
		module.putObject("pdp", pdp);
	}

	private String hmacSigned(JWTClaimsSet claims) throws Exception {
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		jwt.sign(new MACSigner(HMAC_SECRET));
		return jwt.serialize();
	}

	@Test
	public void validSignedWithMatchingIssuer_succeeds() throws Exception {
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer(PDP_ISSUER).build()));
		module.runSequence();
		assertEquals(Result.UNKNOWN, module.getResult());
	}

	@Test
	public void missingIssuer_failsResult() throws Exception {
		// ValidatePDPSignedMetadataIss is called continue-on-failure with explicit FAILURE, so the test result is
		// FAILED but no exception is thrown.
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().subject("not-an-issuer").build()));
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("MUST contain an `iss` (issuer) claim"));
	}

	@Test
	public void issuerMismatch_failsResult() throws Exception {
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer("https://other.example.com").build()));
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("issuer mismatch"));
	}

	@Test
	public void notAString_throws() {
		JsonObject pdp = new JsonObject();
		pdp.addProperty("signed_metadata", 123);
		module.putObject("pdp", pdp);
		// ExtractPDPSignedMetadata is called stop-on-failure, so the sequence aborts with a TestFailureException.
		assertThrows(TestFailureException.class, () -> module.runSequence());
	}

	@Test
	public void notParseable_throws() {
		putSignedMetadata("this-is-not-a-jwt");
		assertThrows(TestFailureException.class, () -> module.runSequence());
	}

	@Test
	public void algNone_failsResult() {
		// A structurally valid unsecured JWT (alg=none, empty signature). It parses, so ExtractPDPSignedMetadata
		// passes, but ValidatePDPSignedMetadataAlg rejects 'none' (continue-on-failure with explicit FAILURE).
		String header = Base64URL.encode("{\"alg\":\"none\"}").toString();
		String payload = Base64URL.encode("{\"iss\":\"" + PDP_ISSUER + "\"}").toString();
		putSignedMetadata(header + "." + payload + ".");
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("Invalid PDP signed_metadata alg"));
	}

	@Test
	public void expiredExp_failsResult() throws Exception {
		Date past = new Date(System.currentTimeMillis() - 3600_000L);
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer(PDP_ISSUER).expirationTime(past).build()));
		module.runSequence();
		assertEquals(Result.FAILED, module.getResult());
	}

	@Test
	public void nestedSignedMetadataClaim_warns() throws Exception {
		// A signed_metadata JWT MUST NOT itself carry a signed_metadata claim; this is flagged as a WARNING.
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder()
			.issuer(PDP_ISSUER)
			.claim("signed_metadata", "nested-value")
			.build()));
		module.runSequence();
		assertEquals(Result.WARNING, module.getResult());
	}

	@PublishTestModule(
		testName = "ValidateDiscoverySignedMetadata Unit Test Module",
		displayName = "ValidateDiscoverySignedMetadata Unit Test Module",
		profile = "UNIT-TEST"
	)
	public static class Harness extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}

		void runSequence() {
			call(sequence(ValidateDiscoverySignedMetadata.class));
		}

		void putObject(String key, JsonObject value) {
			env.putObject(key, value);
		}
	}
}
