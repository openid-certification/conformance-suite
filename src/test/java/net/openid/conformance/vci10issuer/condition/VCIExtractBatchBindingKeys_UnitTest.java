package net.openid.conformance.vci10issuer.condition;

import com.authlete.sd.SDJWT;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.testapp.VciMdocUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIExtractBatchBindingKeys_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIExtractBatchSdJwtBindingKeys sdJwtCond;

	private VCIExtractBatchMdocBindingKeys mdocCond;

	@BeforeEach
	public void setUp() {
		sdJwtCond = new VCIExtractBatchSdJwtBindingKeys();
		sdJwtCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		mdocCond = new VCIExtractBatchMdocBindingKeys();
		mdocCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putCredentials(String... credentials) {
		JsonArray list = new JsonArray();
		for (String credential : credentials) {
			list.add(credential);
		}
		JsonObject extracted = new JsonObject();
		extracted.add("list", list);
		env.putObject("extracted_credentials", extracted);
	}

	private String createSdJwt(ECKey cnfKey) throws Exception {
		ECKey issuerKey = new ECKeyGenerator(Curve.P_256).generate();
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
			.issuer("https://issuer.example.com")
			.claim("vct", "https://credentials.example.com/identity");
		if (cnfKey != null) {
			claims.claim("cnf", Map.of("jwk", cnfKey.toPublicJWK().toJSONObject()));
		}
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("dc+sd-jwt")).build(),
			claims.build());
		jwt.sign(new ECDSASigner(issuerKey));
		return new SDJWT(jwt.serialize(), List.of()).toString();
	}

	@Test
	public void testSdJwt_extractsCnfKeysInOrder() throws Exception {
		ECKey key1 = new ECKeyGenerator(Curve.P_256).generate();
		ECKey key2 = new ECKeyGenerator(Curve.P_256).generate();
		putCredentials(createSdJwt(key1), createSdJwt(key2));

		assertDoesNotThrow(() -> sdJwtCond.execute(env));

		JsonArray keys = env.getObject("vci_batch_binding_keys").getAsJsonArray("keys");
		assertEquals(2, keys.size());
		assertEquals(key1.toPublicJWK().computeThumbprint(),
			JWK.parse(keys.get(0).getAsJsonObject().toString()).computeThumbprint());
		assertEquals(key2.toPublicJWK().computeThumbprint(),
			JWK.parse(keys.get(1).getAsJsonObject().toString()).computeThumbprint());
	}

	@Test
	public void testSdJwt_failsWhenCnfMissing() throws Exception {
		putCredentials(createSdJwt(null));

		assertThrows(ConditionError.class, () -> sdJwtCond.execute(env));
	}

	@Test
	public void testMdoc_extractsDeviceKeys() throws Exception {
		ECKey key1 = new ECKeyGenerator(Curve.P_256).generate();
		ECKey key2 = new ECKeyGenerator(Curve.P_256).generate();
		putCredentials(
			VciMdocUtils.createMdocCredential(key1.toJSONString(), "org.iso.18013.5.1.mDL", null),
			VciMdocUtils.createMdocCredential(key2.toJSONString(), "org.iso.18013.5.1.mDL", null));

		assertDoesNotThrow(() -> mdocCond.execute(env));

		JsonArray keys = env.getObject("vci_batch_binding_keys").getAsJsonArray("keys");
		assertEquals(2, keys.size());
		assertEquals(key1.toPublicJWK().computeThumbprint(),
			JWK.parse(keys.get(0).getAsJsonObject().toString()).computeThumbprint());
		assertEquals(key2.toPublicJWK().computeThumbprint(),
			JWK.parse(keys.get(1).getAsJsonObject().toString()).computeThumbprint());
	}

	@Test
	public void testMdoc_failsOnGarbage() {
		putCredentials("bm90LWNib3I");

		assertThrows(ConditionError.class, () -> mdocCond.execute(env));
	}
}
