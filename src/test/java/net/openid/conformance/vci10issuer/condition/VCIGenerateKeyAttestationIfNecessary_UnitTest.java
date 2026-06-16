package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIGenerateKeyAttestationIfNecessary_UnitTest {

	// A self-contained EC signing key (with an x5c chain, which the condition's signJWT call requires)
	// used as the Key Attestation signer. Only the signature header carries the x5c; the cert is not
	// otherwise validated here.
	private static final String KEY_ATTESTATION_JWKS = """
		{
		  "keys": [
		    {
		      "kty": "EC",
		      "crv": "P-256",
		      "alg": "ES256",
		      "use": "sig",
		      "x": "p-_IHEO9b_XZIyW2SHyYrRyMndwcWjGhnhS-yF6HRiY",
		      "y": "_6IzIjawRYvQLdrypBlCqeBh27jR2tLNUq8h86deoe8",
		      "d": "Gkmh-vjcuC8QStQqLqM_PhJQUp8KepSGGL2-stl79Bs",
		      "kid": "ct_key_attestation_key",
		      "x5c": [
		        "MIICTDCCAdKgAwIBAgIUPlAaWKujE4TvY8sCwXmyDMGgOIwwCgYIKoZIzj0EAwIwLDEqMCgGA1UEAwwhT3BlbklENFZDSSBDb25mb3JtYW5jZSBUZXN0cyBSb290MB4XDTI2MDExNTE2NTQyNFoXDTI4MDQxOTE2NTQyNFowUTELMAkGA1UEBhMCREUxFzAVBgNVBAoMDkV4YW1wbGUgSXNzdWVyMRAwDgYDVQQLDAdPSUQ0VkNJMRcwFQYDVQQDDA5pc3N1ZXIuZXhhbXBsZTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABKfvyBxDvW/12SMltkh8mK0cjJ3cHFoxoZ4Uvsheh0Ym/6IzIjawRYvQLdrypBlCqeBh27jR2tLNUq8h86deoe+jgawwgakwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCB4AwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB0GA1UdDgQWBBSNQHXEutjrfQDfbTgLG0mHepGesjAfBgNVHSMEGDAWgBTgt/z+s54ZDXsVA/YQLaW4RI7WajAqBgNVHREEIzAhgg5pc3N1ZXIuZXhhbXBsZYIJbG9jYWxob3N0hwR/AAABMAoGCCqGSM49BAMCA2gAMGUCMQC24WF0JjXEH0MuirdaXckJuxQUR2N7m3CO2WnUvnmnvEVUfgrUB0G78SFL0LDbuHECMByQ90GH0dB94Z2/4D6f4uDm0j9m6LHTEM0XrW9JcGT2fDMfVEMgUYrMod6yHWbgSw=="
		      ]
		    }
		  ]
		}
		""";

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIGenerateKeyAttestationIfNecessary cond;

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VCIGenerateKeyAttestationIfNecessary();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();

		ECKey attestedKey = new ECKeyGenerator(Curve.P_256).keyID("attested-key-1").generate();
		JsonObject clientJwks = new JsonObject();
		JsonArray keys = new JsonArray();
		keys.add(JsonParser.parseString(attestedKey.toJSONString()));
		clientJwks.add("keys", keys);
		env.putObject("client_jwks", clientJwks);

		env.putObject("vci_credential_configuration", new JsonObject());
		env.putObject("vci_key_attestation_jwks", JsonParser.parseString(KEY_ATTESTATION_JWKS).getAsJsonObject());
	}

	/** Sets vci_proof_type to an "attestation" proof type carrying the given key_attestations_required object. */
	private void putAttestationProofType(JsonObject keyAttestationsRequired) {
		JsonObject proofType = new JsonObject();
		if (keyAttestationsRequired != null) {
			proofType.add("key_attestations_required", keyAttestationsRequired);
		}
		env.putString("vci_proof_type_key", "attestation");
		env.putObject("vci_proof_type", proofType);
	}

	private static JsonObject required(JsonArray keyStorage, JsonArray userAuthentication) {
		JsonObject o = new JsonObject();
		if (keyStorage != null) {
			o.add("key_storage", keyStorage);
		}
		if (userAuthentication != null) {
			o.add("user_authentication", userAuthentication);
		}
		return o;
	}

	private static JsonArray arr(String... values) {
		JsonArray a = new JsonArray();
		for (String v : values) {
			a.add(v);
		}
		return a;
	}

	private JWTClaimsSet generatedClaims() throws Exception {
		String jwt = env.getString("key_attestation_jwt");
		return SignedJWT.parse(jwt).getJWTClaimsSet();
	}

	@Test
	public void highLevelRequirement_echoedIntoAttestation() throws Exception {
		putAttestationProofType(required(arr("iso_18045_high"), arr("iso_18045_high")));

		assertDoesNotThrow(() -> cond.execute(env));

		JWTClaimsSet claims = generatedClaims();
		assertEquals(List.of("iso_18045_high"), claims.getStringListClaim("key_storage"));
		assertEquals(List.of("iso_18045_high"), claims.getStringListClaim("user_authentication"));
	}

	@Test
	public void multipleAcceptedValues_allEchoed() throws Exception {
		putAttestationProofType(required(
			arr("iso_18045_high", "iso_18045_enhanced-basic"),
			arr("iso_18045_high")));

		assertDoesNotThrow(() -> cond.execute(env));

		JWTClaimsSet claims = generatedClaims();
		assertEquals(List.of("iso_18045_high", "iso_18045_enhanced-basic"), claims.getStringListClaim("key_storage"));
		assertEquals(List.of("iso_18045_high"), claims.getStringListClaim("user_authentication"));
	}

	@Test
	public void emptyRequirement_omitsBothClaims() throws Exception {
		// The emulated issuer advertises "key_attestations_required": {} - neither component is required,
		// so neither claim is asserted (both are OPTIONAL in the attestation).
		putAttestationProofType(new JsonObject());

		assertDoesNotThrow(() -> cond.execute(env));

		JWTClaimsSet claims = generatedClaims();
		assertNull(claims.getClaim("key_storage"));
		assertNull(claims.getClaim("user_authentication"));
	}

	@Test
	public void onlyKeyStorageRequired_onlyKeyStorageAsserted() throws Exception {
		putAttestationProofType(required(arr("iso_18045_high"), null));

		assertDoesNotThrow(() -> cond.execute(env));

		JWTClaimsSet claims = generatedClaims();
		assertEquals(List.of("iso_18045_high"), claims.getStringListClaim("key_storage"));
		assertNull(claims.getClaim("user_authentication"));
	}

	@Test
	public void noKeyAttestationRequiredAndNotAttestationProofType_skips() {
		JsonObject proofType = new JsonObject();
		env.putString("vci_proof_type_key", "jwt");
		env.putObject("vci_proof_type", proofType);

		assertDoesNotThrow(() -> cond.execute(env));

		assertNull(env.getString("key_attestation_jwt"));
	}

	@Test
	public void copyRequiredComponent_ignoresNonArrayValue() {
		// Defensive: a malformed key_storage that is not an array must not be copied (and must not throw).
		JsonObject keyAttestationsRequired = new JsonObject();
		keyAttestationsRequired.addProperty("key_storage", "iso_18045_high");
		JsonObject claims = new JsonObject();

		cond.copyRequiredComponent(keyAttestationsRequired, claims, "key_storage");

		assertFalse(claims.has("key_storage"));
	}

	@Test
	public void copyRequiredComponent_copiesArrayValue() {
		JsonObject keyAttestationsRequired = required(arr("iso_18045_high"), null);
		JsonObject claims = new JsonObject();

		cond.copyRequiredComponent(keyAttestationsRequired, claims, "key_storage");

		assertTrue(claims.get("key_storage").isJsonArray());
		assertEquals(arr("iso_18045_high"), claims.getAsJsonArray("key_storage"));
	}
}
