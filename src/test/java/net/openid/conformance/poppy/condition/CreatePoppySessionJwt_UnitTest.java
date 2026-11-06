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
public class CreatePoppySessionJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreatePoppySessionJwt cond;

	@BeforeEach
	public void setUp() {
		cond = new CreatePoppySessionJwt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private com.nimbusds.jose.jwk.ECKey key;

	@BeforeEach
	public void setUpKey() {
		GeneratePoppyDelegatedKey gen = new GeneratePoppyDelegatedKey();
		gen.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		gen.execute(env);
		env.putString("client_id", "https://h/agent.json");
		env.putString("poppy_issuer", "https://as.example");
		env.putString("poppy_sid", "sid1");
		env.putString("poppy_user_id", "usr_abcdefghij");
		env.putString("poppy_delegation", "a.b.c");
		env.putString("protected_resource_url", "https://api.example/x?y=1#f");
		env.putString("poppy_resource_request_method", "GET");
	}

	private com.nimbusds.jwt.SignedJWT run() throws Exception {
		cond.execute(env);
		key = com.nimbusds.jose.jwk.ECKey.parse(env.getObject("poppy_delegated_key").toString());
		return com.nimbusds.jwt.SignedJWT.parse(env.getString("poppy_session_jwt"));
	}

	@Test
	public void buildsSessionJwt() throws Exception {
		JsonObject at = new JsonObject();
		at.addProperty("value", "tok");
		at.addProperty("type", "Bearer");
		env.putObject("access_token", at);
		com.nimbusds.jwt.SignedJWT jwt = run();
		assertEquals("poppy-session+jwt", jwt.getHeader().getType().getType());
		assertEquals(com.nimbusds.jose.JWSAlgorithm.ES256, jwt.getHeader().getAlgorithm());
		assertEquals(key.computeThumbprint().toString(), jwt.getHeader().getKeyID());
		assertEquals("a.b.c", jwt.getHeader().getCustomParam("poppy_dlg"));
		com.nimbusds.jwt.JWTClaimsSet c = jwt.getJWTClaimsSet();
		assertEquals("https://h/agent.json", c.getIssuer());
		assertEquals(java.util.List.of("https://as.example"), c.getAudience());
		assertEquals("sid1", c.getStringClaim("sid"));
		assertEquals("usr_abcdefghij", c.getSubject());
		assertEquals("GET", c.getStringClaim("htm"));
		assertEquals("https://api.example/x", c.getStringClaim("htu"));
		assertTrue(Math.abs(System.currentTimeMillis() - c.getIssueTime().getTime()) < 5000);
		assertNotNull(c.getJWTID());
		String expectedAth = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
			java.security.MessageDigest.getInstance("SHA-256").digest("tok".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
		assertEquals(expectedAth, c.getStringClaim("ath"));
		assertTrue(jwt.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(key.toPublicJWK())));
	}

	@Test
	public void athAbsentWithoutAccessToken() throws Exception {
		com.nimbusds.jwt.SignedJWT jwt = run();
		assertNull(jwt.getJWTClaimsSet().getClaim("ath"));
	}

	@Test
	public void jtiIsUnique() throws Exception {
		String first = run().getJWTClaimsSet().getJWTID();
		assertNotEquals(first, run().getJWTClaimsSet().getJWTID());
	}
}
