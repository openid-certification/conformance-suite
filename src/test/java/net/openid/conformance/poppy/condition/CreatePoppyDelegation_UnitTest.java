package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class CreatePoppyDelegation_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreatePoppyDelegation cond;

	@BeforeEach
	public void setUp() {
		cond = new CreatePoppyDelegation();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private com.nimbusds.jose.jwk.ECKey signing;

	private void setUpEnv(String alg) throws Exception {
		signing = new com.nimbusds.jose.jwk.gen.ECKeyGenerator(com.nimbusds.jose.jwk.Curve.P_256)
			.keyID("sign-1").algorithm(new com.nimbusds.jose.JWSAlgorithm(alg)).generate();
		com.nimbusds.jose.jwk.JWKSet set = new com.nimbusds.jose.jwk.JWKSet(signing);
		env.putObject("client_jwks", JsonParser.parseString(set.toString(false)).getAsJsonObject());
		env.putObject("client_public_jwks", JsonParser.parseString(set.toPublicJWKSet().toString()).getAsJsonObject());
		env.putString("client_id", "https://h/agent.json");
		GeneratePoppyDelegatedKey gen2 = new GeneratePoppyDelegatedKey();
		gen2.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		gen2.execute(env);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void producesVerifiableDelegation() throws Exception {
		setUpEnv("ES256");
		cond.execute(env);
		com.nimbusds.jwt.SignedJWT jwt = com.nimbusds.jwt.SignedJWT.parse(env.getString("poppy_delegation"));
		assertEquals("poppy-delegation+jwt", jwt.getHeader().getType().getType());
		assertEquals(com.nimbusds.jose.JWSAlgorithm.ES256, jwt.getHeader().getAlgorithm());
		assertEquals("sign-1", jwt.getHeader().getKeyID());
		com.nimbusds.jwt.JWTClaimsSet c = jwt.getJWTClaimsSet();
		assertEquals("https://h/agent.json", c.getIssuer());
		java.util.Map<String, Object> jwk = (java.util.Map<String, Object>) ((java.util.Map<String, Object>) c.getClaim("cnf")).get("jwk");
		com.nimbusds.jose.jwk.ECKey delegated = com.nimbusds.jose.jwk.ECKey.parse(env.getObject("poppy_delegated_key").toString());
		assertEquals(delegated.toPublicJWK().toJSONObject(), jwk);
		assertFalse(jwk.containsKey("d"));
		long iat = c.getIssueTime().getTime();
		assertTrue(Math.abs(System.currentTimeMillis() - iat) < 5000);
		assertEquals(3600_000L, c.getExpirationTime().getTime() - iat);
		assertTrue(c.getJWTID().length() >= 16);
		assertTrue(jwt.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(signing.toPublicJWK())));
	}

	@Test
	public void failsWithoutEs256Key() throws Exception {
		setUpEnv("ES256");
		com.nimbusds.jose.jwk.ECKey es384Labelled = new com.nimbusds.jose.jwk.ECKey.Builder(signing).algorithm(com.nimbusds.jose.JWSAlgorithm.ES384).build();
		env.putObject("client_jwks", JsonParser.parseString(new com.nimbusds.jose.jwk.JWKSet(es384Labelled).toString(false)).getAsJsonObject());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
