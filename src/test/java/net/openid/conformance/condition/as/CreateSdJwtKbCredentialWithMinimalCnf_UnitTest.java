package net.openid.conformance.condition.as;

import com.authlete.sd.SDJWT;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateSdJwtKbCredentialWithMinimalCnf_UnitTest {

	private static final String SIGNING_JWK = """
		{
		    "kty": "EC",
		    "d": "y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0",
		    "use": "sig",
		    "crv": "P-256",
		    "kid": "5H1WLeSx55tMW6JNlvqMfg3O_E0eQPqB8jDSoUn6oiI",
		    "x": "0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc",
		    "y": "ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA",
		    "alg": "ES256"
		}""";

	private final Environment env = new Environment();

	private CreateSdJwtKbCredentialWithMinimalCnf cond;

	@BeforeEach
	public void setUp() {
		cond = new CreateSdJwtKbCredentialWithMinimalCnf();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("config", "credential.signing_jwk", SIGNING_JWK);
	}

	@Test
	public void testCreateSdJwt_stripsOptionalMetadataFromCnfJwk() throws Exception {
		ECKey holderKey = new ECKeyGenerator(Curve.P_256)
			.keyID("holder-key")
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.generate();

		// evaluate() uses the overload that also takes the credential claims
		String credential = cond.createSdJwt(env, holderKey.toPublicJWK(), holderKey, "urn:eudi:pid:1", null);

		Map<String, Object> cnf = SignedJWT.parse(SDJWT.parse(credential).getCredentialJwt())
			.getJWTClaimsSet().getJSONObjectClaim("cnf");
		@SuppressWarnings("unchecked")
		Map<String, Object> cnfJwk = (Map<String, Object>) cnf.get("jwk");
		assertEquals(Set.of("kty", "crv", "x", "y"), cnfJwk.keySet());
	}
}
