package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyStatusListTokenSignatureUsingEmbeddedJwk_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VerifyStatusListTokenSignatureUsingEmbeddedJwk cond;

	@BeforeEach
	public void setUp() {
		cond = new VerifyStatusListTokenSignatureUsingEmbeddedJwk();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsWhenNoStatusListToken() {
		cond.execute(env);
		assertFalse(env.containsObject("status_list_token"));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingServerJwks() throws Exception {
		RSAKey signingKey = new RSAKeyGenerator(2048)
			.keyID("status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();

		env.putObject("server_jwks",
			JsonParser.parseString(new JWKSet(signingKey.toPublicJWK()).toString()).getAsJsonObject());
		putStatusListToken(createSignedStatusListToken(signingKey));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsInvalidSignature() throws Exception {
		RSAKey trustedKey = new RSAKeyGenerator(2048)
			.keyID("trusted-status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();
		RSAKey attackerKey = new RSAKeyGenerator(2048)
			.keyID("attacker-status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();

		env.putObject("server_jwks",
			JsonParser.parseString(new JWKSet(trustedKey.toPublicJWK()).toString()).getAsJsonObject());
		putStatusListToken(createSignedStatusListToken(attackerKey));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsWhenNeitherEmbeddedJwkNorServerJwks() throws Exception {
		RSAKey signingKey = new RSAKeyGenerator(2048)
			.keyID("status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();

		putStatusListToken(createSignedStatusListToken(signingKey));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putStatusListToken(String jwt) throws Exception {
		env.putObject("status_list_token",
			net.openid.conformance.util.JWTUtil.jwtStringToJsonObjectForEnvironment(jwt));
	}

	private String createSignedStatusListToken(RSAKey signingKey) throws Exception {
		TokenStatusList tokenStatusList = TokenStatusList.create(
			new byte[] { (byte) TokenStatusList.Status.VALID.getTypeValue() }, 1);
		Instant now = Instant.now();

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.subject("https://issuer.example.com/statuslists/1")
			.issueTime(java.util.Date.from(now))
			.expirationTime(java.util.Date.from(now.plusSeconds(300)))
			.claim("status_list", Map.of("bits", 1L, "lst", tokenStatusList.encodeStatusList()))
			.build();

		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
			claimsSet);
		jwt.sign(new RSASSASigner(signingKey.toPrivateKey()));
		return jwt.serialize();
	}
}
