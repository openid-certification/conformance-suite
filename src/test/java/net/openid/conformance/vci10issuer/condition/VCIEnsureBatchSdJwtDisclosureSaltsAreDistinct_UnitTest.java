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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureBatchSdJwtDisclosureSaltsAreDistinct_UnitTest {

	private VCIEnsureBatchSdJwtDisclosureSaltsAreDistinct cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private ECKey issuerKey;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VCIEnsureBatchSdJwtDisclosureSaltsAreDistinct();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		issuerKey = new ECKeyGenerator(Curve.P_256).generate();
	}

	private String createSdJwt(List<Disclosure> disclosures) throws Exception {
		SDObjectBuilder builder = new SDObjectBuilder();
		for (Disclosure disclosure : disclosures) {
			builder.putSDClaim(disclosure);
		}
		builder.putClaim("iss", "https://issuer.example.com");
		builder.putClaim("vct", "https://credentials.example.com/identity");

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
	public void testEvaluate_passesWhenSaltsDiffer() throws Exception {
		// Disclosure(claimName, claimValue) generates a fresh random salt
		putCredentials(
			createSdJwt(List.of(new Disclosure("given_name", "Erika"), new Disclosure("family_name", "Mustermann"))),
			createSdJwt(List.of(new Disclosure("given_name", "Erika"), new Disclosure("family_name", "Mustermann"))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenSaltReusedAcrossCredentials() throws Exception {
		putCredentials(
			createSdJwt(List.of(new Disclosure("fixed-salt-value", "given_name", "Erika"))),
			createSdJwt(List.of(new Disclosure("fixed-salt-value", "given_name", "Erika"))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesWhenNoDisclosures() throws Exception {
		putCredentials(
			createSdJwt(List.of()),
			createSdJwt(List.of()));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenCredentialIsNotAnSdJwt() {
		putCredentials("not-an-sd-jwt", "also-not-an-sd-jwt");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
