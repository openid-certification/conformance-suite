package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ValidatePoppyDelegation_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePoppyDelegation cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePoppyDelegation();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private final PoppyJwtFixtures fx = new PoppyJwtFixtures();

	private void runSession(String sessionJwt) {
		JsonObject client = new JsonObject();
		client.add("jwks", fx.clientJwks());
		env.putObject("client", client);
		env.putString("poppy_session_jwt", sessionJwt);
		cond.execute(env);
	}

	private void run(String delegation) {
		runSession(fx.session(fx.sessionClaims(null), delegation));
	}

	private void runDelegation(java.util.Map<String, Object> claims) {
		run(fx.delegation(claims));
	}

	private java.util.Map<String, Object> claims() {
		return fx.delegationClaims();
	}

	@Test
	public void validDelegationPasses() {
		run(fx.delegation());
		assertEquals(PoppyJwtFixtures.CLIENT_ID, OIDFJSON.getString(env.getObject("poppy_delegation_claims").get("iss")));
		assertEquals(OIDFJSON.getString(env.getObject("poppy_delegated_public_jwk").get("x")), fx.delegatedKey.getX().toString());
	}

	@Test
	public void sessionWithoutDelegationFails() {
		String noDlg = PoppyJwtFixtures.sign(JWSAlgorithm.ES256, PoppyJwtFixtures.SESSION_TYP, fx.delegatedKey.getKeyID(), null,
			fx.sessionClaims(null), PoppyJwtFixtures.ec(fx.delegatedKey));
		ConditionError e = assertThrows(ConditionError.class, () -> runSession(noDlg));
		assertTrue(e.getMessage().contains("without a delegation"));
	}

	@Test
	public void unknownKidFails() {
		String d = PoppyJwtFixtures.sign(JWSAlgorithm.ES256, PoppyJwtFixtures.DELEGATION_TYP, "other", null, claims(), PoppyJwtFixtures.ec(fx.agentKey));
		assertThrows(ConditionError.class, () -> run(d));
	}

	@Test
	public void signatureByAnotherKeyFails() throws Exception {
		ECKey other = new ECKeyGenerator(Curve.P_256).generate();
		String d = PoppyJwtFixtures.sign(JWSAlgorithm.ES256, PoppyJwtFixtures.DELEGATION_TYP, "k1", null, claims(), PoppyJwtFixtures.ec(other));
		assertThrows(ConditionError.class, () -> run(d));
	}

	@Test
	public void wrongTypFails() {
		String d = PoppyJwtFixtures.sign(JWSAlgorithm.ES256, "JWT", "k1", null, claims(), PoppyJwtFixtures.ec(fx.agentKey));
		assertThrows(ConditionError.class, () -> run(d));
	}

	@Test
	public void rs256Fails() {
		String d = PoppyJwtFixtures.sign(JWSAlgorithm.RS256, PoppyJwtFixtures.DELEGATION_TYP, "rsa1", null, claims(), PoppyJwtFixtures.rsa(fx.rsaKey));
		assertThrows(ConditionError.class, () -> run(d));
	}

	@Test
	public void algNoneFails() {
		java.util.Base64.Encoder enc = java.util.Base64.getUrlEncoder().withoutPadding();
		String header = enc.encodeToString("{\"alg\":\"none\",\"typ\":\"poppy-delegation+jwt\",\"kid\":\"k1\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		String payload = enc.encodeToString(new Payload(claims()).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
		assertThrows(ConditionError.class, () -> run(header + "." + payload + "."));
	}

	@Test
	public void ps384SignedByRsaKeyPasses() {
		run(PoppyJwtFixtures.rsaPs384Delegation(fx.rsaKey, claims()));
		assertNotNull(env.getObject("poppy_delegation_claims"));
	}

	@Test
	public void issMismatchFails() {
		java.util.Map<String, Object> c = claims();
		c.put("iss", "https://evil.example/agent.json");
		assertThrows(ConditionError.class, () -> runDelegation(c));
	}

	@Test
	public void missingJtiFails() {
		java.util.Map<String, Object> c = claims();
		c.remove("jti");
		assertThrows(ConditionError.class, () -> runDelegation(c));
	}

	@Test
	public void missingIatFails() {
		java.util.Map<String, Object> c = claims();
		c.remove("iat");
		assertThrows(ConditionError.class, () -> runDelegation(c));
	}

	@Test
	public void missingExpFails() {
		java.util.Map<String, Object> c = claims();
		c.remove("exp");
		assertThrows(ConditionError.class, () -> runDelegation(c));
	}

	@Test
	public void expiredFails() {
		java.util.Map<String, Object> c = claims();
		long now = java.time.Instant.now().getEpochSecond();
		c.put("iat", now - 7200);
		c.put("exp", now - 3600);
		assertThrows(ConditionError.class, () -> runDelegation(c));
	}

	@Test
	public void lifetimeOver24HoursFails() {
		java.util.Map<String, Object> c = claims();
		long now = java.time.Instant.now().getEpochSecond();
		c.put("iat", now);
		c.put("exp", now + 86401);
		assertThrows(ConditionError.class, () -> runDelegation(c));
	}

	@Test
	public void lifetimeOf24HoursPasses() {
		java.util.Map<String, Object> c = claims();
		long now = java.time.Instant.now().getEpochSecond();
		c.put("iat", now);
		c.put("exp", now + 86400);
		assertDoesNotThrow(() -> runDelegation(c));
	}

	@Test
	public void privateKeyInCnfFails() {
		java.util.Map<String, Object> c = claims();
		c.put("cnf", java.util.Map.of("jwk", fx.delegatedKey.toJSONObject()));
		assertThrows(ConditionError.class, () -> runDelegation(c));
	}

	@Test
	public void forbiddenClaimsFail() {
		for (String name : new String[] {"sub", "aud", "agent_id"}) {
			java.util.Map<String, Object> c = claims();
			c.put(name, "x");
			assertThrows(ConditionError.class, () -> runDelegation(c), name);
		}
	}
}
