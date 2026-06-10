package net.openid.conformance.vci10issuer.condition;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.authlete.sd.SDObjectBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureBatchSdJwtCredentialDatasetsMatch_UnitTest {

	private VCIEnsureBatchSdJwtCredentialDatasetsMatch cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private ECKey issuerKey;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VCIEnsureBatchSdJwtCredentialDatasetsMatch();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		issuerKey = new ECKeyGenerator(Curve.P_256).generate();
	}

	private String createSdJwt(String vct, Map<String, Object> disclosedClaims, long iat) throws Exception {
		ECKey holderKey = new ECKeyGenerator(Curve.P_256).generate();

		SDObjectBuilder builder = new SDObjectBuilder();
		List<Disclosure> disclosures = new ArrayList<>();
		for (Map.Entry<String, Object> entry : disclosedClaims.entrySet()) {
			disclosures.add(builder.putSDClaim(entry.getKey(), entry.getValue()));
		}
		builder.putClaim("iss", "https://issuer.example.com");
		builder.putClaim("vct", vct);
		builder.putClaim("iat", iat);
		builder.putClaim("jti", UUID.randomUUID().toString());
		Map<String, Object> cnf = new HashMap<>();
		cnf.put("jwk", holderKey.toPublicJWK().toJSONObject());
		builder.putClaim("cnf", cnf);

		JWTClaimsSet claimsSet = JWTClaimsSet.parse(builder.build());
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("dc+sd-jwt")).build(),
			claimsSet);
		jwt.sign(new ECDSASigner(issuerKey));

		return new SDJWT(jwt.serialize(), disclosures).toString();
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

	@Test
	public void testEvaluate_passesWhenOnlyCnfJtiAndIatDiffer() throws Exception {
		Map<String, Object> claims = Map.of("given_name", "Erika", "family_name", "Mustermann");
		putCredentials(
			createSdJwt("https://credentials.example.com/identity", claims, 1000),
			createSdJwt("https://credentials.example.com/identity", claims, 2000));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenDisclosedClaimDiffers() throws Exception {
		putCredentials(
			createSdJwt("https://credentials.example.com/identity",
				Map.of("given_name", "Erika", "family_name", "Mustermann"), 1000),
			createSdJwt("https://credentials.example.com/identity",
				Map.of("given_name", "Max", "family_name", "Mustermann"), 1000));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenDisclosedClaimMissing() throws Exception {
		putCredentials(
			createSdJwt("https://credentials.example.com/identity",
				Map.of("given_name", "Erika", "family_name", "Mustermann"), 1000),
			createSdJwt("https://credentials.example.com/identity",
				Map.of("given_name", "Erika"), 1000));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenVctDiffers() throws Exception {
		Map<String, Object> claims = Map.of("given_name", "Erika");
		putCredentials(
			createSdJwt("https://credentials.example.com/identity", claims, 1000),
			createSdJwt("https://credentials.example.com/other", claims, 1000));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenCredentialIsNotAnSdJwt() {
		putCredentials("not-an-sd-jwt", "also-not-an-sd-jwt");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenNoCredentials() {
		putCredentials();

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
