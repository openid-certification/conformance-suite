package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyEntityStatementSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyEntityStatementSignature condition;
	private ECKey ecKey;

	@BeforeEach
	public void setUp() throws Exception {
		condition = new VerifyEntityStatementSignature();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		ecKey = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("test-kid")
			.generate();
	}

	@Test
	public void acceptsValidSignatureWithMatchingKid() throws Exception {
		String jwt = signWithKid(ecKey, "test-kid");
		putEnv(jwt, ecKey);
		condition.execute(env);
	}

	@Test
	public void rejectsMissingKidInHeader() throws Exception {
		String jwt = signWithKid(ecKey, null);
		putEnv(jwt, ecKey);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsKidMismatch() throws Exception {
		String jwt = signWithKid(ecKey, "wrong-kid");
		putEnv(jwt, ecKey);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	private String signWithKid(ECKey key, String kid) throws Exception {
		JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.ES256);
		if (kid != null) {
			builder.keyID(kid);
		}
		builder.type(new JOSEObjectType("entity-statement+jwt"));

		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issuer("https://example.com")
			.subject("https://example.com")
			.build();

		SignedJWT signedJWT = new SignedJWT(builder.build(), claims);
		signedJWT.sign(new ECDSASigner(key));
		return signedJWT.serialize();
	}

	private void putEnv(String jwt, ECKey key) {
		JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(new JWKSet(key));
		env.putObject("ec_jwks", publicJwks);

		JsonObject jwtObj = new JsonObject();
		jwtObj.addProperty("value", jwt);
		env.putObject("federation_response_jwt", jwtObj);
	}
}
