package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateSdJwtCredentialSignatureUsingX5c_UnitTest {

	private static final String NON_SELF_SIGNED_X5C = "MIICTDCCAdKgAwIBAgIUPlAaWKujE4TvY8sCwXmyDMGgOIwwCgYIKoZIzj0EAwIwLDEqMCgGA1UEAwwhT3BlbklENFZDSSBDb25mb3JtYW5jZSBUZXN0cyBSb290MB4XDTI2MDExNTE2NTQyNFoXDTI4MDQxOTE2NTQyNFowUTELMAkGA1UEBhMCREUxFzAVBgNVBAoMDkV4YW1wbGUgSXNzdWVyMRAwDgYDVQQLDAdPSUQ0VkNJMRcwFQYDVQQDDA5pc3N1ZXIuZXhhbXBsZTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABKfvyBxDvW/12SMltkh8mK0cjJ3cHFoxoZ4Uvsheh0Ym/6IzIjawRYvQLdrypBlCqeBh27jR2tLNUq8h86deoe+jgawwgakwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCB4AwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB0GA1UdDgQWBBSNQHXEutjrfQDfbTgLG0mHepGesjAfBgNVHSMEGDAWgBTgt/z+s54ZDXsVA/YQLaW4RI7WajAqBgNVHREEIzAhgg5pc3N1ZXIuZXhhbXBsZYIJbG9jYWxob3N0hwR/AAABMAoGCCqGSM49BAMCA2gAMGUCMQC24WF0JjXEH0MuirdaXckJuxQUR2N7m3CO2WnUvnmnvEVUfgrUB0G78SFL0LDbuHECMByQ90GH0dB94Z2/4D6f4uDm0j9m6LHTEM0XrW9JcGT2fDMfVEMgUYrMod6yHWbgSw==";

	private static final String NON_SELF_SIGNED_JWK = """
		{
			"kty": "EC",
			"crv": "P-256",
			"alg": "ES256",
			"use": "sig",
			"x": "p-_IHEO9b_XZIyW2SHyYrRyMndwcWjGhnhS-yF6HRiY",
			"y": "_6IzIjawRYvQLdrypBlCqeBh27jR2tLNUq8h86deoe8",
			"d": "Gkmh-vjcuC8QStQqLqM_PhJQUp8KepSGGL2-stl79Bs",
			"kid": "ct_client_attester_key",
			"x5c": [
				"MIICTDCCAdKgAwIBAgIUPlAaWKujE4TvY8sCwXmyDMGgOIwwCgYIKoZIzj0EAwIwLDEqMCgGA1UEAwwhT3BlbklENFZDSSBDb25mb3JtYW5jZSBUZXN0cyBSb290MB4XDTI2MDExNTE2NTQyNFoXDTI4MDQxOTE2NTQyNFowUTELMAkGA1UEBhMCREUxFzAVBgNVBAoMDkV4YW1wbGUgSXNzdWVyMRAwDgYDVQQLDAdPSUQ0VkNJMRcwFQYDVQQDDA5pc3N1ZXIuZXhhbXBsZTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABKfvyBxDvW/12SMltkh8mK0cjJ3cHFoxoZ4Uvsheh0Ym/6IzIjawRYvQLdrypBlCqeBh27jR2tLNUq8h86deoe+jgawwgakwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCB4AwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB0GA1UdDgQWBBSNQHXEutjrfQDfbTgLG0mHepGesjAfBgNVHSMEGDAWgBTgt/z+s54ZDXsVA/YQLaW4RI7WajAqBgNVHREEIzAhgg5pc3N1ZXIuZXhhbXBsZYIJbG9jYWxob3N0hwR/AAABMAoGCCqGSM49BAMCA2gAMGUCMQC24WF0JjXEH0MuirdaXckJuxQUR2N7m3CO2WnUvnmnvEVUfgrUB0G78SFL0LDbuHECMByQ90GH0dB94Z2/4D6f4uDm0j9m6LHTEM0XrW9JcGT2fDMfVEMgUYrMod6yHWbgSw=="
			]
		}
		""";

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateSdJwtCredentialSignatureUsingX5c cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateSdJwtCredentialSignatureUsingX5c();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_acceptsValidCredentialSignature() throws Exception {
		ECKey signingKey = (ECKey) JWK.parse(NON_SELF_SIGNED_JWK);
		env.putObject("sdjwt", createCredentialObject(createSignedCredential(signingKey)));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsInvalidCredentialSignature() throws Exception {
		ECKey signingKey = (ECKey) JWK.parse(NON_SELF_SIGNED_JWK);
		String invalidCredential = invalidateSignature(createSignedCredential(signingKey));
		env.putObject("sdjwt", createCredentialObject(invalidCredential));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsMissingX5c() throws Exception {
		ECKey signingKey = (ECKey) JWK.parse(NON_SELF_SIGNED_JWK);
		JsonObject credential = createCredentialObject(createSignedCredential(signingKey));
		credential.getAsJsonObject("credential").getAsJsonObject("header").remove("x5c");
		env.putObject("sdjwt", credential);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private JsonObject createCredentialObject(String credentialJwt) {
		JsonObject header = new JsonObject();
		header.addProperty("alg", "ES256");
		header.addProperty("kid", "ct_client_attester_key");
		JsonArray x5c = new JsonArray();
		x5c.add(NON_SELF_SIGNED_X5C);
		header.add("x5c", x5c);

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "https://issuer.example.com");
		claims.addProperty("iat", Instant.now().getEpochSecond());

		JsonObject credential = new JsonObject();
		credential.addProperty("value", credentialJwt);
		credential.add("header", header);
		credential.add("claims", claims);

		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		return sdjwt;
	}

	private String createSignedCredential(ECKey signingKey) throws Exception {
		Instant now = Instant.now();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.issuer("https://issuer.example.com")
			.issueTime(java.util.Date.from(now))
			.expirationTime(java.util.Date.from(now.plusSeconds(300)))
			.claim("vct", "example_credential")
			.build();

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID())
			.x509CertChain(signingKey.getX509CertChain())
			.build();

		SignedJWT jwt = new SignedJWT(header, claimsSet);
		jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));
		return jwt.serialize();
	}

	private String invalidateSignature(String jwt) {
		char replacement = jwt.charAt(jwt.length() - 1) == 'A' ? 'B' : 'A';
		return jwt.substring(0, jwt.length() - 1) + replacement;
	}
}
