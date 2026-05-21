package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for gl1820: a signed credential issuer metadata JWT whose
 * {@code credential_configurations_supported} uses a reverse-DNS credential id with dots
 * (e.g. {@code eu.europa.ec.eudi.pid.1}) must not blow up the event-log write when the condition
 * reports a claim-validation failure via {@code error(..., args("payload", payload))}.
 *
 * <p>The event log is a real (spied) {@link TestInstanceEventLog} from
 * {@link BsonEncoding#testInstanceEventLog()}, so every log payload is driven through the same
 * Gson -> BSON -> MappingMongoConverter encode path production uses. Before the fix the dotted map
 * key tripped {@code MappingMongoConverter#potentiallyEscapeMapKey} and the real {@code iat}
 * diagnostic was masked by a {@code MappingException}.
 */
@ExtendWith(MockitoExtension.class)
public class VCIDecodeSignedCredentialIssuerMetadata_UnitTest {

	private static final String ISSUER = "https://issuer.example.com";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ECKey signingKey;

	private VCIDecodeSignedCredentialIssuerMetadata cond;

	@BeforeEach
	public void setUp() throws Exception {
		signingKey = new ECKeyGenerator(Curve.P_256)
			.keyID("metadata-signing-key")
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.generate();

		cond = new VCIDecodeSignedCredentialIssuerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	/** Sign {@code payloadJson} as an issuer-metadata JWT and place it where the condition reads it. */
	private void putSignedMetadata(String payloadJson) throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("openidvci-issuer-metadata+jwt"))
			.keyID(signingKey.getKeyID())
			.jwk(signingKey.toPublicJWK())
			.build();
		SignedJWT jwt = new SignedJWT(header, JWTClaimsSet.parse(payloadJson));
		jwt.sign(new ECDSASigner(signingKey));

		JsonObject response = new JsonObject();
		response.addProperty("body", jwt.serialize());
		env.putObject("credential_issuer_metadata_endpoint_response", response);

		JsonObject vci = new JsonObject();
		vci.addProperty("credential_issuer_url", ISSUER);
		JsonObject config = new JsonObject();
		config.add("vci", vci);
		env.putObject("config", config);
	}

	@Test
	public void dottedCredentialConfigurationKeyInErrorPayloadIsEncodableForMongo() throws Exception {
		// iat in epoch MILLISECONDS (a common issuer bug) makes the condition reject the metadata
		// via error("Invalid 'iat' ...", args("payload", payload)). payload here is the Nimbus
		// claim-set Map and carries the dotted credential_configurations_supported key.
		String payloadJson = """
			{
			  "sub": "%s",
			  "iat": 1747000000000,
			  "credential_configurations_supported": {
			    "eu.europa.ec.eudi.pid.1": {
			      "format": "dc+sd-jwt"
			    }
			  }
			}
			""".formatted(ISSUER);

		putSignedMetadata(payloadJson);

		// The failure that surfaces must be the iat diagnostic, encoded cleanly into the log -
		// not a MappingException from an un-escaped dotted map key.
		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("iat"),
			"expected the iat validation failure to surface, got: " + error.getMessage());
	}

	@Test
	public void validSignedMetadataWithDottedKeysDecodesAndLogsCleanly() throws Exception {
		// The success path logs credential_issuer_metadata (also carrying the dotted credential id);
		// it must encode cleanly too.
		long iatSeconds = System.currentTimeMillis() / 1000L - 60;
		String payloadJson = """
			{
			  "sub": "%s",
			  "iat": %d,
			  "credential_issuer": "%s",
			  "credential_configurations_supported": {
			    "eu.europa.ec.eudi.pid.1": {
			      "format": "dc+sd-jwt"
			    }
			  }
			}
			""".formatted(ISSUER, iatSeconds, ISSUER);

		putSignedMetadata(payloadJson);

		assertDoesNotThrow(() -> cond.execute(env));
	}
}
