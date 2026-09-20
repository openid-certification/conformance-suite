package net.openid.conformance.condition.client;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class InvalidateClientAssertionSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private InvalidateClientAssertionSignature cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new InvalidateClientAssertionSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_signatureNoLongerVerifies() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256),
			new JWTClaimsSet.Builder().issuer("client").subject("client").build());
		signed.sign(new ECDSASigner(key));
		SignedJWT original = SignedJWT.parse(signed.serialize());
		env.putString("client_assertion", signed.serialize());

		cond.execute(env);

		SignedJWT tampered = SignedJWT.parse(env.getString("client_assertion"));
		assertThat(tampered.getParsedParts()[0]).isEqualTo(original.getParsedParts()[0]);
		assertThat(tampered.getParsedParts()[1]).isEqualTo(original.getParsedParts()[1]);
		assertThat(tampered.verify(new ECDSAVerifier(key.toECPublicKey()))).isFalse();
	}

	@Test
	public void testEvaluate_missingAssertion() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_notAJwt() {
		env.putString("client_assertion", "not-a-jwt");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
