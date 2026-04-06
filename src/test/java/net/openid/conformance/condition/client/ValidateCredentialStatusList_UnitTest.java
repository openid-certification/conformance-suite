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
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateCredentialStatusList_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private TestableValidateCredentialStatusList cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new TestableValidateCredentialStatusList();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject statusList = new JsonObject();
		statusList.addProperty("idx", 0);
		statusList.addProperty("uri", "https://issuer.example.com/statuslists/1");

		JsonObject status = new JsonObject();
		status.add("status_list", statusList);

		JsonObject claims = new JsonObject();
		claims.add("status", status);
		env.putObject("sdjwt", "credential.claims", claims);
	}

	@Test
	public void testEvaluate_acceptsValidSignedStatusListToken() throws Exception {
		RSAKey signingKey = new RSAKeyGenerator(2048)
			.keyID("status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();

		env.putObject("server_jwks", JsonParser.parseString(new JWKSet(signingKey.toPublicJWK()).toString()).getAsJsonObject());
		cond.setResponse(ResponseEntity.ok(createSignedStatusListToken(signingKey, TokenStatusList.Status.VALID)));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsStatusListTokenWithInvalidSignature() throws Exception {
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

		env.putObject("server_jwks", JsonParser.parseString(new JWKSet(trustedKey.toPublicJWK()).toString()).getAsJsonObject());
		cond.setResponse(ResponseEntity.ok(createSignedStatusListToken(attackerKey, TokenStatusList.Status.VALID)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private String createSignedStatusListToken(RSAKey signingKey, TokenStatusList.Status credentialStatus) throws Exception {
		TokenStatusList tokenStatusList = TokenStatusList.create(new byte[] { (byte) credentialStatus.getTypeValue() }, 1);
		Instant now = Instant.now();

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.subject("https://issuer.example.com/statuslists/1")
			.issueTime(java.util.Date.from(now))
			.expirationTime(java.util.Date.from(now.plusSeconds(300)))
			.claim("status_list", Map.of("bits", 1L, "lst", tokenStatusList.encodeStatusList()))
			.build();

		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.RS256)
				.keyID(signingKey.getKeyID())
				.build(),
			claimsSet
		);
		jwt.sign(new RSASSASigner(signingKey.toPrivateKey()));
		return jwt.serialize();
	}

	private static class TestableValidateCredentialStatusList extends ValidateCredentialStatusList {
		private ResponseEntity<String> response;

		void setResponse(ResponseEntity<String> response) {
			this.response = response;
		}

		@Override
		protected ResponseEntity<String> fetchStatusListToken(Environment env, String uri) {
			return response;
		}
	}
}
