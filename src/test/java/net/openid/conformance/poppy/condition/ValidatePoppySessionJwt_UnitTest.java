package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
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

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ValidatePoppySessionJwt_UnitTest {

	private static final String TOKEN = "tok";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private final PoppyJwtFixtures fx = new PoppyJwtFixtures();

	private ValidatePoppySessionJwt cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePoppySessionJwt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void prepare(String jwt, String incomingToken) {
		env.putString("poppy_session_jwt", jwt);
		env.putObject("poppy_delegated_public_jwk", JsonParser.parseString(fx.delegatedKey.toPublicJWK().toJSONString()).getAsJsonObject());
		env.putString("issuer", PoppyJwtFixtures.ISSUER);
		env.putString("poppy_incoming_client_id", PoppyJwtFixtures.CLIENT_ID);
		JsonObject req = new JsonObject();
		req.addProperty("method", "GET");
		req.addProperty("request_url", "https://h:8443/test/a/root-well-known/api/me?x=1");
		env.putObject("incoming_request", req);
		env.putString("access_token", TOKEN);
		if (incomingToken != null) {
			env.putString("incoming_access_token", incomingToken);
		}
	}

	private void run(Map<String, Object> claims) {
		prepare(fx.session(claims), TOKEN);
		cond.execute(env);
	}

	private void runWith(Consumer<Map<String, Object>> mutate) {
		Map<String, Object> c = fx.sessionClaims(TOKEN);
		mutate.accept(c);
		run(c);
	}

	@Test
	public void validPasses() {
		run(fx.sessionClaims(TOKEN));
		assertEquals("sid1", net.openid.conformance.testmodule.OIDFJSON.getString(env.getObject("poppy_session_claims").get("sid")));
	}

	@Test
	public void validWithoutTokenPasses() {
		prepare(fx.session(fx.sessionClaims(null)), null);
		env.removeNativeValue("access_token");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void signatureByDifferentKeyFails() throws Exception {
		ECKey other = new ECKeyGenerator(Curve.P_256).generate();
		String jwt = PoppyJwtFixtures.sign(JWSAlgorithm.ES256, PoppyJwtFixtures.SESSION_TYP, fx.delegatedKey.getKeyID(), fx.delegation(),
			fx.sessionClaims(TOKEN), PoppyJwtFixtures.ec(other));
		prepare(jwt, TOKEN);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void kidNotThumbprintFails() {
		String jwt = PoppyJwtFixtures.sign(JWSAlgorithm.ES256, PoppyJwtFixtures.SESSION_TYP, "not-the-thumbprint", fx.delegation(),
			fx.sessionClaims(TOKEN), PoppyJwtFixtures.ec(fx.delegatedKey));
		prepare(jwt, TOKEN);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void wrongTypFails() {
		String jwt = PoppyJwtFixtures.sign(JWSAlgorithm.ES256, "JWT", fx.delegatedKey.getKeyID(), fx.delegation(),
			fx.sessionClaims(TOKEN), PoppyJwtFixtures.ec(fx.delegatedKey));
		prepare(jwt, TOKEN);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void es384Fails() throws Exception {
		ECKey p384 = new ECKeyGenerator(Curve.P_384).generate();
		String jwt = PoppyJwtFixtures.sign(JWSAlgorithm.ES384, PoppyJwtFixtures.SESSION_TYP, p384.computeThumbprint().toString(), fx.delegation(),
			fx.sessionClaims(TOKEN), PoppyJwtFixtures.ec(p384));
		prepare(jwt, TOKEN);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void audAsTokenEndpointFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("aud", "https://as.example/token")));
	}

	@Test
	public void audAsArrayFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("aud", java.util.List.of(PoppyJwtFixtures.ISSUER))));
	}

	@Test
	public void htmMismatchFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("htm", "POST")));
	}

	@Test
	public void htuWithQueryFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("htu", "https://h:8443/test/a/root-well-known/api/me?x=1")));
	}

	@Test
	public void htuWithDifferentPathFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("htu", "https://h:8443/test/a/root-well-known/api/other")));
	}

	@Test
	public void iat120SecondsAgoFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("iat", Instant.now().getEpochSecond() - 120)));
	}

	@Test
	public void iat61SecondsAheadFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("iat", Instant.now().getEpochSecond() + 61)));
	}

	@Test
	public void iat30SecondsEitherWayPasses() {
		assertDoesNotThrow(() -> runWith(c -> c.put("iat", Instant.now().getEpochSecond() - 30)));
		assertDoesNotThrow(() -> runWith(c -> c.put("iat", Instant.now().getEpochSecond() + 30)));
	}

	@Test
	public void athMissingWithTokenFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.remove("ath")));
	}

	@Test
	public void athForAnotherTokenFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("ath", PoppyJwtFixtures.ath("other"))));
	}

	@Test
	public void athWithoutIncomingTokenFails() {
		prepare(fx.session(fx.sessionClaims(TOKEN)), null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void issNotIncomingClientIdFails() {
		assertThrows(ConditionError.class, () -> runWith(c -> c.put("iss", "https://other.example/agent.json")));
	}

	@Test
	public void missingSidSubJtiFail() {
		for (String name : new String[] {"sid", "sub", "jti"}) {
			assertThrows(ConditionError.class, () -> runWith(c -> c.remove(name)), name);
		}
	}

	@Test
	public void tokenTheSuiteDidNotIssueFails() {
		prepare(fx.session(fx.sessionClaims("forged")), "forged");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
