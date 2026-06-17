package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.util.Base64URL;
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
class ValidateDiscoverySignedMetadata_UnitTest {

	// 256-bit secret for HS256.
	private static final byte[] HMAC_SECRET = "0123456789abcdef0123456789abcdef".getBytes();

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateDiscoverySignedMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateDiscoverySignedMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putSignedMetadata(String token) {
		JsonObject pdp = new JsonObject();
		if (token != null) {
			pdp.addProperty("signed_metadata", token);
		}
		env.putObject("pdp", pdp);
	}

	private String hmacSigned(JWTClaimsSet claims) throws Exception {
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		jwt.sign(new MACSigner(HMAC_SECRET));
		return jwt.serialize();
	}

	@Test
	public void noSignedMetadata_succeeds() {
		putSignedMetadata(null);
		cond.execute(env);
	}

	@Test
	public void validHmacSignedWithIssuer_succeeds() throws Exception {
		// No embedded verification key: the signature cannot be cryptographically
		// verified, but the structural checks (parse, alg, iss) pass.
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().issuer("https://pdp.example.com").build()));
		cond.execute(env);
	}

	@Test
	public void missingIssuer_fails() throws Exception {
		putSignedMetadata(hmacSigned(new JWTClaimsSet.Builder().subject("not-an-issuer").build()));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void notAString_fails() {
		JsonObject pdp = new JsonObject();
		pdp.addProperty("signed_metadata", 123);
		env.putObject("pdp", pdp);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void notParseable_fails() {
		putSignedMetadata("this-is-not-a-jwt");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void algNone_fails() {
		String header = Base64URL.encode("{\"alg\":\"none\"}").toString();
		String payload = Base64URL.encode("{\"iss\":\"https://pdp.example.com\"}").toString();
		putSignedMetadata(header + "." + payload + ".");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
