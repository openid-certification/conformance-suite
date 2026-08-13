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
 *   <li>stop-on-failure sub-conditions ({@link ExtractPDPSignedMetadata}, {@link ValidatePDPSignedMetadataAlg},
 *       {@link VerifyAuthzenSignedMetadataSignature}, {@link ValidatePDPSignedMetadataIss} and the
 *       {@code exp} / {@code nbf} checks, whose JWT must not be applied over the plain metadata when it
 *       has expired or is not yet valid) throw a {@link TestFailureException};</li>
 *   <li>continue-on-failure sub-conditions ({@code iat}, nested signed_metadata) leave the module
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

	private static final String THIRD_PARTY_ISSUER = "https://other.example.com";

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

	/** Declare a third-party attester as the expected `signed_metadata` issuer. */
	private void configureMetadataIssuer(String issuer) {
		JsonObject config = new JsonObject();
		JsonObject pdpCfg = new JsonObject();
		pdpCfg.addProperty("policy_decision_point", PDP_ISSUER);
		pdpCfg.addProperty("metadata_issuer", issuer);
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
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().subject("not-an-issuer").build()));
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("MUST contain an `iss` (issuer) claim"));
	}

	@Test
	public void issuerMismatch_throws() throws Exception {
		// No 'Signed Metadata Issuer' is configured, so the metadata is expected to be self-attested.
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer(THIRD_PARTY_ISSUER).build()));
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("issuer mismatch"));
	}

	@Test
	public void configuredMetadataIssuer_succeeds() throws Exception {
		// AuthZEN §11.8 permits the attesting party to differ from the PDP; the tester declares it via
		// the 'Signed Metadata Issuer' config field.
		configureMetadataIssuer(THIRD_PARTY_ISSUER);
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer(THIRD_PARTY_ISSUER).build()));
		module.runSequence();
		assertEquals(Result.UNKNOWN, module.getResult());
	}

	@Test
	public void notAString_throws() {
		JsonObject pdp = new JsonObject();
		pdp.addProperty("signed_metadata", 123);
		module.putObject("pdp", pdp);
		// ExtractPDPSignedMetadata is called stop-on-failure, so the sequence aborts with a TestFailureException.
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("must be a JWT string"));
	}

	@Test
	public void notParseable_throws() {
		putSignedMetadata("this-is-not-a-jwt");
		assertThrows(TestFailureException.class, () -> module.runSequence());
	}

	@Test
	public void algNone_failsResult() {
		// A structurally valid unsecured JWT (alg=none, empty signature). It parses, so ExtractPDPSignedMetadata
		// passes, and ValidatePDPSignedMetadataAlg rejects 'none' stop-on-failure.
		String header = Base64URL.encode("{\"alg\":\"none\"}").toString();
		String payload = Base64URL.encode("{\"iss\":\"" + PDP_ISSUER + "\"}").toString();
		putSignedMetadata(header + "." + payload + ".");
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("`alg: none`"), e.getMessage());
	}

	@Test
	public void unknownAlg_throws() {
		// An `alg` the JOSE library cannot map to a key type. Previously this passed the alg check and
		// then crashed VerifyAuthzenSignedMetadataSignature with a NullPointerException.
		String header = Base64URL.encode("{\"alg\":\"FOO256\"}").toString();
		String payload = Base64URL.encode("{\"iss\":\"" + PDP_ISSUER + "\"}").toString();
		putSignedMetadata(header + "." + payload + ".AAAA");
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("not a registered JWS signature or MAC algorithm"));
	}

	@Test
	public void jweAlg_throws() {
		// 'dir' is a JWE key management algorithm, not a JWS one, but KeyType.forAlgorithm() maps it
		// to oct — so it used to pass the alg check and fail later in signature verification.
		String header = Base64URL.encode("{\"alg\":\"dir\"}").toString();
		String payload = Base64URL.encode("{\"iss\":\"" + PDP_ISSUER + "\"}").toString();
		putSignedMetadata(header + "." + payload + ".AAAA");
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("not a registered JWS signature or MAC algorithm"));
	}

	@Test
	public void expiredExp_aborts() throws Exception {
		// Expired signed metadata MUST NOT be applied over the plain metadata, so the sequence stops
		// rather than continuing into ApplySignedMetadataPrecedence.
		Date past = new Date(System.currentTimeMillis() - 3600_000L);
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer(PDP_ISSUER).expirationTime(past).build()));
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("exp claim is invalid"));
	}

	@Test
	public void futureNbf_aborts() throws Exception {
		Date future = new Date(System.currentTimeMillis() + 3600_000L);
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer(PDP_ISSUER).notBeforeTime(future).build()));
		Throwable e = assertThrows(TestFailureException.class, () -> module.runSequence());
		assertTrue(e.getMessage().contains("nbf claim is invalid"));
	}

	@Test
	public void futureIat_failsResultWithoutAborting() throws Exception {
		// RFC 7519 attaches no validity requirement to `iat`, so unlike exp/nbf it does not make the
		// metadata unusable: the failure is recorded but the sequence runs to completion.
		Date future = new Date(System.currentTimeMillis() + 3600_000L);
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer(PDP_ISSUER).issueTime(future).build()));
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
