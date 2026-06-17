package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class VerifyAuthzenSignedMetadataSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VerifyAuthzenSignedMetadataSignature cond;

	@BeforeEach
	public void setUp() {
		cond = new VerifyAuthzenSignedMetadataSignature();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putSignedMetadata(String token) {
		JsonObject pdp = new JsonObject();
		if (token != null) {
			pdp.addProperty("signed_metadata", token);
		}
		env.putObject("pdp", pdp);
	}

	private void putConfigJwks(JsonObject jwks) {
		JsonObject config = new JsonObject();
		JsonObject pdpCfg = new JsonObject();
		if (jwks != null) {
			pdpCfg.add("jwks", jwks);
		}
		config.add("pdp", pdpCfg);
		env.putObject("config", config);
	}

	private static ECKey genKey() throws Exception {
		return new ECKeyGenerator(Curve.P_256).generate();
	}

	private static String sign(ECKey key, String iss) throws Exception {
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256),
			new JWTClaimsSet.Builder().issuer(iss).build());
		jwt.sign(new ECDSASigner(key));
		return jwt.serialize();
	}

	private static JsonObject jwkSet(ECKey key) {
		return JsonParser.parseString(new JWKSet(key.toPublicJWK()).toString()).getAsJsonObject();
	}

	private static JsonObject bareJwk(ECKey key) {
		return JsonParser.parseString(key.toPublicJWK().toString()).getAsJsonObject();
	}

	@Test
	public void noSignedMetadata_succeeds() throws Exception {
		putSignedMetadata(null);
		putConfigJwks(jwkSet(genKey()));
		cond.execute(env);
	}

	@Test
	public void signedMetadataButNoJwksConfigured_skips() throws Exception {
		putSignedMetadata(sign(genKey(), "https://pdp.example.com"));
		putConfigJwks(null);
		cond.execute(env);
	}

	@Test
	public void validSignatureAgainstConfiguredJwks_succeeds() throws Exception {
		ECKey key = genKey();
		putSignedMetadata(sign(key, "https://pdp.example.com"));
		putConfigJwks(jwkSet(key));
		cond.execute(env);
	}

	@Test
	public void validSignatureAgainstConfiguredBareJwk_succeeds() throws Exception {
		ECKey key = genKey();
		putSignedMetadata(sign(key, "https://pdp.example.com"));
		putConfigJwks(bareJwk(key));
		cond.execute(env);
	}

	@Test
	public void wrongKey_fails() throws Exception {
		putSignedMetadata(sign(genKey(), "https://pdp.example.com"));
		putConfigJwks(jwkSet(genKey()));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void tamperedSignature_fails() throws Exception {
		ECKey key = genKey();
		String[] parts = sign(key, "https://pdp.example.com").split("\\.");
		// Flip the first signature char (the high bits of the first byte) so the
		// decoded signature bytes definitely change — flipping the last base64url
		// char can leave the decoded bytes unchanged.
		String firstChar = parts[2].startsWith("A") ? "B" : "A";
		String tampered = parts[0] + "." + parts[1] + "." + firstChar + parts[2].substring(1);
		putSignedMetadata(tampered);
		putConfigJwks(jwkSet(key));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
