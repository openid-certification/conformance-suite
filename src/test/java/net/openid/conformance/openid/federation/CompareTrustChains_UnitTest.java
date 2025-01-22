package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class CompareTrustChains_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CompareTrustChains cond;

	static String LEAF = "https://leaf.com";
	static String INTERMEDIATE = "https://intermediate.com";
	static String TRUST_ANCHOR = "https:///trust-anchor.com";

	static String AUD = null;
	static Date NOW = Date.from(Instant.now());


	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setUp() {
		cond = new CompareTrustChains();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		doAnswer(i -> {
			Map<String, Object> logMap = i.getArgument(1);
			List<String> diffs = (List<String>) logMap.get("diffs");
			if (diffs != null) {
				diffs.forEach(diff -> System.out.println(diff));
			}
			return null;
		}).when(eventLog).log(anyString(), anyMap());
	}

	// First, a set of tests to verify that the condition throws on standard entity statement claims

	@Test
	public void it_does_not_throw_when_comparing_two_identical_trust_chains() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate, trustAnchor),
			List.of(leaf, intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		cond.execute(env);
	}

	@Test
	public void it_throws_when_the_trust_chains_have_different_lengths() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate, trustAnchor),
			List.of(intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The number of entries in the trust chains does not match", ex.getMessage());
	}

	@Test
	public void it_throws_if_one_of_the_entries_cannot_be_parsed() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String intermediate1 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String intermediate2 = "NOT_A_JWS";
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate1, trustAnchor),
			List.of(leaf, intermediate2, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: Failed to parse at least one of the entries in the trust chains", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_iss() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		String intermediateJwks = createJWKSet();

		String intermediate1Iss = "https://intermediate1.com";
		String intermediate1 = createEntityStatementJWS(intermediate1Iss, LEAF, AUD, NOW, NOW, intermediateJwks);

		String intermediate2Iss = "https://intermediate2.com";
		String intermediate2 = createEntityStatementJWS(intermediate2Iss, LEAF, AUD, NOW, NOW, intermediateJwks);

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate1, trustAnchor),
			List.of(leaf, intermediate2, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_sub() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		String intermediateJwks = createJWKSet();

		String intermediate1Sub = "https://leaf1.com";
		String intermediate1 = createEntityStatementJWS(INTERMEDIATE, intermediate1Sub, AUD, NOW, NOW, intermediateJwks);

		String intermediate2Sub = "https://leaf2.com";
		String intermediate2 = createEntityStatementJWS(INTERMEDIATE, intermediate2Sub, AUD, NOW, NOW, intermediateJwks);

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate1, trustAnchor),
			List.of(leaf, intermediate2, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	public void it_throws_on_diverging_aud() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		String intermediateJwks = createJWKSet();
		String intermediate1 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD + "1", NOW, NOW, intermediateJwks);
		String intermediate2 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD + "2", NOW, NOW, intermediateJwks);

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate1, trustAnchor),
			List.of(leaf, intermediate2, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_jwks() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		String intermediate1 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String intermediate2 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate1, trustAnchor),
			List.of(leaf, intermediate2, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_authority_hints() {
		JsonArray authorityHints1 = new JsonArray();
		authorityHints1.add(INTERMEDIATE);
		JsonArray authorityHints2 = new JsonArray();
		authorityHints2.add(TRUST_ANCHOR);

		String leafJwks = createJWKSet();
		String leaf1 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, authorityHints1, null, null, null, null, null, null, null, null, null);
		String leaf2 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, authorityHints2, null, null, null, null, null, null, null, null, null);
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf1, intermediate, trustAnchor),
			List.of(leaf2, intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_metadata() {
		JsonObject metadata1 = new JsonObject();
		metadata1.add("federation_entity", new JsonObject());
		JsonObject metadata2 = new JsonObject();
		metadata2.add("openid_provider", new JsonObject());

		String leafJwks = createJWKSet();
		String leaf1 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, metadata1, null, null, null, null, null, null, null, null);
		String leaf2 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, metadata2, null, null, null, null, null, null, null, null);
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf1, intermediate, trustAnchor),
			List.of(leaf2, intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_metadata_policy() {
		JsonObject metadataPolicy1 = new JsonObject();
		metadataPolicy1.add("federation_entity", new JsonObject());
		JsonObject metadataPolicy2 = new JsonObject();
		metadataPolicy2.add("openid_provider", new JsonObject());

		String leafJwks = createJWKSet();
		String leaf1 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, metadataPolicy1, null, null, null, null, null, null, null);
		String leaf2 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, metadataPolicy2, null, null, null, null, null, null, null);
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf1, intermediate, trustAnchor),
			List.of(leaf2, intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_constraints() {
		JsonObject constraints1 = new JsonObject();
		constraints1.addProperty("max_path_length", 1);
		JsonObject constraints2 = new JsonObject();
		constraints2.addProperty("max_path_length", 2);

		String leafJwks = createJWKSet();
		String leaf1 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, null, constraints1, null, null, null, null, null, null);
		String leaf2 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, null, constraints2, null, null, null, null, null, null);
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf1, intermediate, trustAnchor),
			List.of(leaf2, intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_crit() {
		JsonArray crit1 = new JsonArray();
		crit1.add("claim1");
		JsonArray crit2 = new JsonArray();
		crit2.add("claim2");

		String leafJwks = createJWKSet();
		String leaf1 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, null, null, crit1, null, null, null, null, null);
		String leaf2 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, null, null, crit2, null, null, null, null, null);
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf1, intermediate, trustAnchor),
			List.of(leaf2, intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_metadata_policy_crit() {
		JsonArray metadataPolicyCrit1 = new JsonArray();
		metadataPolicyCrit1.add("subtract");
		JsonArray metadataPolicyCrit2 = new JsonArray();
		metadataPolicyCrit2.add("multiply");

		String leafJwks = createJWKSet();
		String leaf1 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, null, null, null, metadataPolicyCrit1, null, null, null, null);
		String leaf2 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, null, null, null, metadataPolicyCrit2, null, null, null, null);
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf1, intermediate, trustAnchor),
			List.of(leaf2, intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_trust_mark_issuers() {
		JsonObject trustMarkIssuers1 = new JsonObject();
		JsonArray trustMarkIssuer1 = new JsonArray();
		trustMarkIssuer1.add("https://trust-mark-issuer1.com");
		trustMarkIssuers1.add(INTERMEDIATE, trustMarkIssuer1);

		JsonObject trustMarkIssuers2 = new JsonObject();
		JsonArray trustMarkIssuer2 = new JsonArray();
		trustMarkIssuers2.add(INTERMEDIATE, trustMarkIssuer2);

		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchorJwks = createJWKSet();
		String trustAnchor1 = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, trustAnchorJwks, null, null, null, null, null, null, null, trustMarkIssuers1, null, null);
		String trustAnchor2 = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, trustAnchorJwks, null, null, null, null, null, null, null, trustMarkIssuers2, null, null);

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate, trustAnchor1),
			List.of(leaf, intermediate, trustAnchor2)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_trust_mark_owners() {
		JsonObject trustMarkOwners1 = new JsonObject();
		JsonObject trustMarkOwner1 = new JsonObject();
		trustMarkOwner1.addProperty("sub", INTERMEDIATE);
		trustMarkOwner1.addProperty("jwks", createJWKSet());
		trustMarkOwners1.add(INTERMEDIATE, trustMarkOwner1);

		JsonObject trustMarkOwners2 = new JsonObject();
		JsonObject trustMarkOwner2 = new JsonObject();
		trustMarkOwner2.addProperty("sub", INTERMEDIATE);
		trustMarkOwner2.addProperty("jwks", createJWKSet());
		trustMarkOwners2.add(INTERMEDIATE, trustMarkOwner2);

		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchorJwks = createJWKSet();
		String trustAnchor1 = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, trustAnchorJwks, null, null, null, null, null, null, null, null, trustMarkOwners1, null);
		String trustAnchor2 = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, trustAnchorJwks, null, null, null, null, null, null, null, null, trustMarkOwners2, null);

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate, trustAnchor1),
			List.of(leaf, intermediate, trustAnchor2)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	@Test
	public void it_throws_on_diverging_source_endpoint() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String intermediateJwks = createJWKSet();
		String intermediate1 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, intermediateJwks, null, null, null, null, null, null, null, null, null, "https://source-endpoint1.com");
		String intermediate2 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, intermediateJwks, null, null, null, null, null, null, null, null, null, "https://source-endpoint2.com");
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate1, trustAnchor),
			List.of(leaf, intermediate2, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("CompareTrustChains: The trust chains do not match", ex.getMessage());
	}

	// Second, a set of tests to verify which standard entity statement claims the condition DOES NOT throw on

	@Test
	public void it_currently_DOES_NOT_throw_on_diverging_trust_marks() {
		JsonArray trustMarks1 = new JsonArray();
		JsonObject trustMark1 = new JsonObject();
		trustMark1.addProperty("id", "trustMark1");
		trustMark1.addProperty("trust_mark", createEntityStatementJWS());
		trustMarks1.add(trustMark1);

		JsonArray trustMarks2 = new JsonArray();
		JsonObject trustMark2 = new JsonObject();
		trustMark2.addProperty("id", "trustMark2");
		trustMark2.addProperty("trust_mark", createEntityStatementJWS());
		trustMarks2.add(trustMark2);

		String leafJwks = createJWKSet();
		String leaf1 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, null, null, null, null, trustMarks1, null, null, null);
		String leaf2 = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, leafJwks, null, null, null, null, null, null, trustMarks2, null, null, null);
		String intermediate = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		JsonObject trustChains = getTrustChains(
			List.of(leaf1, intermediate, trustAnchor),
			List.of(leaf2, intermediate, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		cond.execute(env);
	}

	@Test
	public void it_does_NOT_throw_on_diverging_iat_or_exp() {
		String leaf = createEntityStatementJWS(LEAF, LEAF, AUD, NOW, NOW, createJWKSet());
		String trustAnchor = createEntityStatementJWS(TRUST_ANCHOR, INTERMEDIATE, AUD, NOW, NOW, createJWKSet());

		String intermediateJwks = createJWKSet();
		String intermediate1 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD,
			Date.from(NOW.toInstant().plusSeconds(1)), Date.from(NOW.toInstant().plusSeconds(2)), intermediateJwks);
		String intermediate2 = createEntityStatementJWS(INTERMEDIATE, LEAF, AUD,
			Date.from(NOW.toInstant().plusSeconds(3)), Date.from(NOW.toInstant().plusSeconds(4)), intermediateJwks);

		JsonObject trustChains = getTrustChains(
			List.of(leaf, intermediate1, trustAnchor),
			List.of(leaf, intermediate2, trustAnchor)
		);
		env.putObject("trust_chains", trustChains);

		cond.execute(env);
	}

	// Utility methods below

	public JsonObject getTrustChains(List<String> firstTrustChain, List<String> secondTrustChain) {
		JsonObject trustChains = new JsonObject();
		JsonArray firstChain = new JsonArray();
		JsonArray secondChain = new JsonArray();

		firstTrustChain.forEach(firstChain::add);
		secondTrustChain.forEach(secondChain::add);

		trustChains.add("manual", firstChain);
		trustChains.add("resolved", secondChain);

		return trustChains;
	}

	public static String createEntityStatementJWS() {
		return createEntityStatementJWS(LEAF, LEAF, LEAF, NOW, NOW, createJWKSet());
	}

	public static String createEntityStatementJWS(String iss, String sub, String aud, Date iat, Date exp, String jwks) {
		JsonArray authorityHints = null;
		JsonObject metadata = null;
		JsonObject metadataPolicy = null;
		JsonObject constraints = null;
		JsonArray crit = null;
		JsonArray metadataPolicyCrit = null;
		JsonArray trustMarks = null;
		JsonObject trustMarkIssuers = null;
		JsonObject trustMarkOwners = null;
		String sourceEndpoint = null;
		return createEntityStatementJWS(iss, sub, aud, iat, exp, jwks,
			authorityHints, metadata, metadataPolicy, constraints, crit,
			metadataPolicyCrit, trustMarks, trustMarkIssuers, trustMarkOwners, sourceEndpoint);
	}

	public static String createEntityStatementJWS(String iss, String sub, String aud, Date iat, Date exp, String jwks,
												  JsonArray authorityHints, JsonObject metadata, JsonObject metadataPolicy,
												  JsonObject constraints, JsonArray crit, JsonArray metadataPolicyCrit,
												  JsonArray trustMarks, JsonObject trustMarkIssuers, JsonObject trustMarkOwners,
												  String sourceEndpoint) {
		JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();

		claimsSetBuilder
			.issuer(iss)
			.subject(sub)
			.audience(aud)
			.issueTime(iat != null ? iat : Date.from(Instant.now()))
			.expirationTime(exp != null ? exp : Date.from(Instant.now().plusSeconds(3600)));
		claimsSetBuilder.claim("jwks", jwks);
		claimsSetBuilder.claim("authority_hints", authorityHints);
		claimsSetBuilder.claim("metadata", metadata);
		claimsSetBuilder.claim("metadata_policy", metadataPolicy);
		claimsSetBuilder.claim("constraints", constraints);
		claimsSetBuilder.claim("crit", crit);
		claimsSetBuilder.claim("metadata_policy_crit", metadataPolicyCrit);
		claimsSetBuilder.claim("trust_marks", trustMarks);
		claimsSetBuilder.claim("trust_mark_issuers", trustMarkIssuers);
		claimsSetBuilder.claim("trust_mark_owners", trustMarkOwners);
		claimsSetBuilder.claim("source_endpoint", sourceEndpoint);

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID("key1").build();
		SignedJWT signedJWT = new SignedJWT(header, claimsSetBuilder.build());
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(2048);
			RSAPrivateKey privateKey = (RSAPrivateKey) gen.generateKeyPair().getPrivate();
			JWSSigner signer = new RSASSASigner(privateKey);
			signedJWT.sign(signer);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return signedJWT.serialize();
	}

	public static String createJWKSet() {
		try {
			RSAKey rsaJWK = new RSAKeyGenerator(2048)
				.algorithm(JWSAlgorithm.RS256)
				.keyID(UUID.randomUUID().toString())
				.keyUse(KeyUse.SIGNATURE)
				.generate();
			JWKSet jwkSet = new JWKSet(rsaJWK);
			return jwkSet.toString(true);
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
	}

}
