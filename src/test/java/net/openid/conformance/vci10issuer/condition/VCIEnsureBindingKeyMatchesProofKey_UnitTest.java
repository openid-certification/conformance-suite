package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.testapp.VciMdocUtils;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureBindingKeyMatchesProofKey_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIEnsureSdJwtCnfMatchesProofKey sdJwtCond;

	private VCIEnsureMdocDeviceKeyMatchesProofKey mdocCond;

	private ECKey proofKey;

	@BeforeEach
	public void setUp() throws Exception {
		sdJwtCond = new VCIEnsureSdJwtCnfMatchesProofKey();
		sdJwtCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		mdocCond = new VCIEnsureMdocDeviceKeyMatchesProofKey();
		mdocCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		proofKey = new ECKeyGenerator(Curve.P_256).generate();
	}

	private void putJwtProofs(ECKey... keys) throws Exception {
		JsonArray jwts = new JsonArray();
		for (ECKey key : keys) {
			SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.ES256)
					.type(new JOSEObjectType("openid4vci-proof+jwt"))
					.jwk(key.toPublicJWK())
					.build(),
				new JWTClaimsSet.Builder().audience("https://issuer.example.com").claim("nonce", "n").build());
			jwt.sign(new ECDSASigner(key));
			jwts.add(jwt.serialize());
		}
		JsonObject proofs = new JsonObject();
		proofs.add("jwt", jwts);
		env.putObject("credential_request_proofs", proofs);
	}

	private void putAttestationProof(ECKey... attestedKeys) throws Exception {
		ECKey attestationSigningKey = new ECKeyGenerator(Curve.P_256).generate();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.claim("attested_keys", List.of(attestedKeys).stream().map(k -> k.toPublicJWK().toJSONObject()).toList())
			.build();
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256)
				.type(new JOSEObjectType("key-attestation+jwt"))
				.build(),
			claims);
		jwt.sign(new ECDSASigner(attestationSigningKey));
		JsonArray attestations = new JsonArray();
		attestations.add(jwt.serialize());
		JsonObject proofs = new JsonObject();
		proofs.add("attestation", attestations);
		env.putObject("credential_request_proofs", proofs);
	}

	private void putSdJwtWithCnf(ECKey cnfKey) {
		JsonObject claims = new JsonObject();
		if (cnfKey != null) {
			JsonObject cnf = new JsonObject();
			cnf.add("jwk", JsonParser.parseString(cnfKey.toPublicJWK().toJSONString()));
			claims.add("cnf", cnf);
		}
		JsonObject credential = new JsonObject();
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}

	private void putMdocWithDeviceKey(ECKey deviceKey) {
		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			deviceKey.toJSONString(), "org.iso.18013.5.1.mDL", null);
		byte[] bytes = new Base64URL(mdocBase64Url).decode();
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(bytes));
	}

	@Test
	public void testSdJwt_passesWhenCnfMatchesJwtProofKey() throws Exception {
		putJwtProofs(proofKey);
		putSdJwtWithCnf(proofKey);

		assertDoesNotThrow(() -> sdJwtCond.execute(env));
	}

	@Test
	public void testSdJwt_passesWhenCnfMatchesOneOfSeveralProofKeys() throws Exception {
		ECKey otherKey = new ECKeyGenerator(Curve.P_256).generate();
		putJwtProofs(otherKey, proofKey);
		putSdJwtWithCnf(proofKey);

		assertDoesNotThrow(() -> sdJwtCond.execute(env));
	}

	@Test
	public void testSdJwt_passesWhenCnfMatchesAttestedKey() throws Exception {
		putAttestationProof(proofKey);
		putSdJwtWithCnf(proofKey);

		assertDoesNotThrow(() -> sdJwtCond.execute(env));
	}

	@Test
	public void testSdJwt_failsWhenCnfDoesNotMatchProofKey() throws Exception {
		ECKey wrongKey = new ECKeyGenerator(Curve.P_256).generate();
		putJwtProofs(proofKey);
		putSdJwtWithCnf(wrongKey);

		assertThrows(ConditionError.class, () -> sdJwtCond.execute(env));
	}

	@Test
	public void testSdJwt_failsWhenCnfMissing() throws Exception {
		putJwtProofs(proofKey);
		putSdJwtWithCnf(null);

		assertThrows(ConditionError.class, () -> sdJwtCond.execute(env));
	}

	@Test
	public void testMdoc_passesWhenDeviceKeyMatchesProofKey() throws Exception {
		putJwtProofs(proofKey);
		putMdocWithDeviceKey(proofKey);

		assertDoesNotThrow(() -> mdocCond.execute(env));
	}

	@Test
	public void testMdoc_failsWhenDeviceKeyDoesNotMatchProofKey() throws Exception {
		ECKey wrongKey = new ECKeyGenerator(Curve.P_256).generate();
		putJwtProofs(proofKey);
		putMdocWithDeviceKey(wrongKey);

		assertThrows(ConditionError.class, () -> mdocCond.execute(env));
	}
}
