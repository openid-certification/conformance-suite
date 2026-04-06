package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateCredentialStatusListForHaip_UnitTest {

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

	private static final String SELF_SIGNED_JWK = """
		{
			"kty": "EC",
			"crv": "P-256",
			"x": "MeTgsS50BR72Lj--MxFPTL7DNKxClCymdqo1hZ8_09U",
			"y": "5cHwkpG7iLvtsqA41gNowdAt4Ro83vdE-P6eWGmegLc",
			"d": "gwmApx70vcVlRzQid2uY-ooMjtm331NmCvtOuIOr_6I",
			"use": "sig",
			"kid": "x5t-s256-unit-test",
			"alg": "ES256",
			"x5c": [
				"MIIBkjCCATegAwIBAgIUZkRih1mNAs9PfQphhjLx8O2Uej8wCgYIKoZIzj0EAwIwHTEbMBkGA1UEAwwSeDV0LXMyNTYtdW5pdC10ZXN0MCAXDTI2MDIwODE1NTEwMVoYDzIxMjYwMTE1MTU1MTAxWjAdMRswGQYDVQQDDBJ4NXQtczI1Ni11bml0LXRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQx5OCxLnQFHvYuP74zEU9MvsM0rEKULKZ2qjWFnz/T1eXB8JKRu4i77bKgONYDaMHQLeEaPN73RPj+nlhpnoC3o1MwUTAdBgNVHQ4EFgQUQYMPimHGw8fD+nAw5hXN1tLeHE8wHwYDVR0jBBgwFoAUQYMPimHGw8fD+nAw5hXN1tLeHE8wDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEAgyNkETTSsp/nkhXKjNETK4UGQXSayRAFtZ6hJSyKIOUCIQCIW7UskVfn6zliot/KzfmqY1XDjaTf6kzqhv5YBlRmtg=="
			]
		}
		""";

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private TestableValidateCredentialStatusListForHaip cond;

	@BeforeEach
	public void setUp() {
		cond = new TestableValidateCredentialStatusListForHaip();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject statusList = new JsonObject();
		statusList.addProperty("idx", 0);
		statusList.addProperty("uri", "https://issuer.example.com/statuslists/1");

		JsonObject status = new JsonObject();
		status.add("status_list", statusList);

		JsonObject claims = new JsonObject();
		claims.add("status", status);
		env.putObject("sdjwt", "credential.claims", claims);
	}

	@Test
	public void testEvaluate_acceptsValidStatusListTokenWithNonSelfSignedX5c() throws Exception {
		ECKey signingKey = (ECKey) JWK.parse(NON_SELF_SIGNED_JWK);
		cond.setResponse(ResponseEntity.ok(createSignedStatusListToken(signingKey, signingKey.getX509CertChain(), TokenStatusList.Status.VALID)));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsMissingX5c() throws Exception {
		ECKey signingKey = (ECKey) JWK.parse(NON_SELF_SIGNED_JWK);
		cond.setResponse(ResponseEntity.ok(createSignedStatusListToken(signingKey, null, TokenStatusList.Status.VALID)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsSelfSignedLeafCertificate() throws Exception {
		ECKey signingKey = (ECKey) JWK.parse(SELF_SIGNED_JWK);
		cond.setResponse(ResponseEntity.ok(createSignedStatusListToken(signingKey, signingKey.getX509CertChain(), TokenStatusList.Status.VALID)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsTrustAnchorIncludedInX5cChain() throws Exception {
		ECKey signingKey = (ECKey) JWK.parse(NON_SELF_SIGNED_JWK);
		ECKey selfSignedKey = (ECKey) JWK.parse(SELF_SIGNED_JWK);
		List<com.nimbusds.jose.util.Base64> x5cChain = new ArrayList<>(signingKey.getX509CertChain());
		x5cChain.addAll(selfSignedKey.getX509CertChain());

		cond.setResponse(ResponseEntity.ok(createSignedStatusListToken(signingKey, x5cChain, TokenStatusList.Status.VALID)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private String createSignedStatusListToken(ECKey signingKey, List<com.nimbusds.jose.util.Base64> x5cChain, TokenStatusList.Status credentialStatus) throws Exception {
		TokenStatusList tokenStatusList = TokenStatusList.create(new byte[] { (byte) credentialStatus.getTypeValue() }, 1);
		Instant now = Instant.now();

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.subject("https://issuer.example.com/statuslists/1")
			.issueTime(java.util.Date.from(now))
			.expirationTime(java.util.Date.from(now.plusSeconds(300)))
			.claim("status_list", Map.of("bits", 1L, "lst", tokenStatusList.encodeStatusList()))
			.build();

		JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID());
		if (x5cChain != null) {
			headerBuilder.x509CertChain(x5cChain);
		}

		SignedJWT jwt = new SignedJWT(headerBuilder.build(), claimsSet);
		jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));
		return jwt.serialize();
	}

	private static class TestableValidateCredentialStatusListForHaip extends ValidateCredentialStatusListForHaip {
		private ResponseEntity<String> response;

		void setResponse(ResponseEntity<String> response) {
			this.response = response;
		}

		@Override
		protected ResponseEntity<String> fetchStatusListToken(Environment env, String uri) {
			return response;
		}
	}
}
