package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateDpopProofResourceRequest_UnitTest {

	private static final String METHOD = "GET";
	private static final String URL = "https://resource.example.com/resource";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateDpopProofResourceRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateDpopProofResourceRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject incomingRequest = new JsonObject();
		incomingRequest.addProperty("method", METHOD);
		incomingRequest.addProperty("request_url", URL);
		env.putObject("incoming_request", incomingRequest);
	}

	private JsonObject ed25519PublicJwk() throws JOSEException {
		OctetKeyPair keyPair = new OctetKeyPairGenerator(Curve.Ed25519).generate();
		return JsonParser.parseString(keyPair.toPublicJWK().toJSONString()).getAsJsonObject();
	}

	private void addDpopProof(String alg, JsonObject jwk) {
		JsonObject header = new JsonObject();
		header.addProperty("typ", "dpop+jwt");
		header.addProperty("alg", alg);
		header.add("jwk", jwk);

		JsonObject claims = new JsonObject();
		claims.addProperty("jti", "unit-test-jti");
		claims.addProperty("htm", METHOD);
		claims.addProperty("htu", URL);
		claims.addProperty("iat", Instant.now().getEpochSecond());
		claims.addProperty("ath", "unit-test-ath");

		JsonObject proof = new JsonObject();
		proof.add("header", header);
		proof.add("claims", claims);
		env.putObject("incoming_dpop_proof", proof);
	}

	@Test
	public void testEd25519AlgWithEd25519KeyPasses() throws JOSEException {
		addDpopProof("Ed25519", ed25519PublicJwk());
		cond.execute(env);
	}

	@Test
	public void testEdDSAAlgWithEd25519KeyStillPasses() throws JOSEException {
		addDpopProof("EdDSA", ed25519PublicJwk());
		cond.execute(env);
	}

	@Test
	public void testUnsupportedAlgFails() throws JOSEException {
		assertThrows(ConditionError.class, () -> {
			addDpopProof("RS256", ed25519PublicJwk());
			cond.execute(env);
		});
	}
}
