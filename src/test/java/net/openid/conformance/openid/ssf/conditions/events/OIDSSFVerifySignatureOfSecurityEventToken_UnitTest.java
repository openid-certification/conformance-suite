package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The refresh of the transmitter JWKS on an unknown {@code kid} needs a reachable
 * {@code jwks_uri}; these tests cover the decision around it and the verification itself.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFVerifySignatureOfSecurityEventToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private RSAKey publishedKey;

	private RSAKey rotatedKey;

	@BeforeEach
	void setUp() throws Exception {
		publishedKey = new RSAKeyGenerator(2048).keyID("published").generate();
		rotatedKey = new RSAKeyGenerator(2048).keyID("rotated").generate();
		env.putObject("server_jwks", JsonParser.parseString(new JWKSet(publishedKey).toString(false)).getAsJsonObject());
		env.putObject("ssf", new JsonObject());
	}

	private OIDSSFVerifySignatureOfSecurityEventToken createCondition() {
		OIDSSFVerifySignatureOfSecurityEventToken condition = new OIDSSFVerifySignatureOfSecurityEventToken();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void putSet(RSAKey signingKey) throws Exception {
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
			new JWTClaimsSet.Builder().issuer("https://transmitter.example.com").jwtID("jti-1").build());
		jwt.sign(new RSASSASigner(signingKey));
		env.putString("ssf", "verification.jwt", jwt.serialize());
	}

	@Test
	void verifiesASetSignedWithAPublishedKey() throws Exception {
		putSet(publishedKey);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsForAnUnknownKidWhenThereIsNoJwksUriToRefreshFrom() throws Exception {
		putSet(rotatedKey);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void knowsWhichKidsTheJwksContains() {
		JsonObject jwks = env.getObject("server_jwks");
		assertTrue(OIDSSFVerifySignatureOfSecurityEventToken.jwksContainsKid(jwks, "published"));
		assertFalse(OIDSSFVerifySignatureOfSecurityEventToken.jwksContainsKid(jwks, "rotated"));
		assertFalse(OIDSSFVerifySignatureOfSecurityEventToken.jwksContainsKid(new JsonObject(), "published"));
		assertFalse(OIDSSFVerifySignatureOfSecurityEventToken.jwksContainsKid(null, "published"));
	}
}
