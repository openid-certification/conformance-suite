package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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
public class CheckCredentialStatus_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckCredentialStatus cond;

	private RSAKey signingKey;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckCredentialStatus();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		signingKey = new RSAKeyGenerator(2048)
			.keyID("status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();
		// server_jwks isn't used by this condition but is expected in env by convention
		env.putObject("server_jwks",
			com.google.gson.JsonParser.parseString(new JWKSet(signingKey.toPublicJWK()).toString()).getAsJsonObject());

		env.putInteger("status_list_idx", 0);
	}

	@Test
	public void testEvaluate_skipsWhenNoStatusListToken() {
		env.removeObject("server_jwks");
		cond.execute(env);
		assertFalse(env.containsObject("status_list_token"));
	}

	@Test
	public void testEvaluate_acceptsValidStatus() throws Exception {
		putStatusListToken(createSignedStatusListToken(TokenStatusList.Status.VALID));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsInvalidStatus() throws Exception {
		putStatusListToken(createSignedStatusListToken(TokenStatusList.Status.INVALID));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsMissingStatusListClaim() throws Exception {
		Instant now = Instant.now();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.subject("https://issuer.example.com/statuslists/1")
			.issueTime(java.util.Date.from(now))
			.expirationTime(java.util.Date.from(now.plusSeconds(300)))
			.build();

		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
			claims);
		jwt.sign(new RSASSASigner(signingKey.toPrivateKey()));

		putStatusListToken(jwt.serialize());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsMissingBits() throws Exception {
		putStatusListToken(createSignedStatusListTokenWithCustomClaim(Map.of("lst", "AAAA")));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsMissingLst() throws Exception {
		putStatusListToken(createSignedStatusListTokenWithCustomClaim(Map.of("bits", 1L)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsBadBitsType() throws Exception {
		putStatusListToken(createSignedStatusListTokenWithCustomClaim(Map.of("bits", "1", "lst", "AAAA")));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putStatusListToken(String jwtString) throws Exception {
		env.putObject("status_list_token",
			net.openid.conformance.util.JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString));
	}

	private String createSignedStatusListToken(TokenStatusList.Status credentialStatus) throws Exception {
		TokenStatusList tokenStatusList = TokenStatusList.create(
			new byte[] { (byte) credentialStatus.getTypeValue() }, 1);
		return createSignedStatusListTokenWithCustomClaim(
			Map.of("bits", 1L, "lst", tokenStatusList.encodeStatusList()));
	}

	private String createSignedStatusListTokenWithCustomClaim(Map<String, Object> statusListClaim) throws Exception {
		Instant now = Instant.now();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.subject("https://issuer.example.com/statuslists/1")
			.issueTime(java.util.Date.from(now))
			.expirationTime(java.util.Date.from(now.plusSeconds(300)))
			.claim("status_list", statusListClaim)
			.build();

		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
			claims);
		jwt.sign(new RSASSASigner(signingKey.toPrivateKey()));
		return jwt.serialize();
	}
}
