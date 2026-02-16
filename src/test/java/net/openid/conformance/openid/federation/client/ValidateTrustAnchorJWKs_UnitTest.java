package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
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
public class ValidateTrustAnchorJWKs_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateTrustAnchorJWKs condition;

	@BeforeEach
	public void setUp() {
		condition = new ValidateTrustAnchorJWKs();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void acceptsPrivateJwks() throws Exception {
		var key = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("unit-test-key")
			.generate();

		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(key));
		env.putObject("trust_anchor_jwks", privateJwks);

		condition.execute(env);
	}

	@Test
	public void rejectsPublicJwks() throws Exception {
		var key = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("unit-test-key")
			.generate();

		JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(new JWKSet(key));
		env.putObject("trust_anchor_jwks", publicJwks);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
