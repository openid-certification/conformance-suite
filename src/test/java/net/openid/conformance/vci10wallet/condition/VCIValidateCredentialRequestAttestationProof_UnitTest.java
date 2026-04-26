package net.openid.conformance.vci10wallet.condition;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64;
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

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIValidateCredentialRequestAttestationProof_UnitTest {

	private static final String SELF_SIGNED_CERT = "MIIBkjCCATegAwIBAgIUZkRih1mNAs9PfQphhjLx8O2Uej8wCgYIKoZIzj0EAwIwHTEbMBkGA1UEAwwSeDV0LXMyNTYtdW5pdC10ZXN0MCAXDTI2MDIwODE1NTEwMVoYDzIxMjYwMTE1MTU1MTAxWjAdMRswGQYDVQQDDBJ4NXQtczI1Ni11bml0LXRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQx5OCxLnQFHvYuP74zEU9MvsM0rEKULKZ2qjWFnz/T1eXB8JKRu4i77bKgONYDaMHQLeEaPN73RPj+nlhpnoC3o1MwUTAdBgNVHQ4EFgQUQYMPimHGw8fD+nAw5hXN1tLeHE8wHwYDVR0jBBgwFoAUQYMPimHGw8fD+nAw5hXN1tLeHE8wDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEAgyNkETTSsp/nkhXKjNETK4UGQXSayRAFtZ6hJSyKIOUCIQCIW7UskVfn6zliot/KzfmqY1XDjaTf6kzqhv5YBlRmtg==";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIValidateCredentialRequestAttestationProof cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIValidateCredentialRequestAttestationProof();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void haipMissingX5cSetsInvalidProofBeforeJwksFallback() throws Exception {
		env.putString("vci", "fapi_profile", "vci_haip");

		String keyAttestationJwt = createKeyAttestationJwt(null);

		assertThrows(ConditionError.class, () -> cond.validateKeyAttestation(env, "jwt", keyAttestationJwt));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
		assertEquals("Key attestation JWT header MUST contain an x5c claim per HAIP §4.5.1",
			env.getString("vci", "credential_error_response.body.error_description"));
	}

	@Test
	public void validatesNestedKeyAttestationX5cChainBeforeTrustingLeafKey() throws Exception {
		String keyAttestationJwt = createKeyAttestationJwt(List.of(Base64.from(SELF_SIGNED_CERT)));

		assertThrows(ConditionError.class, () -> cond.validateKeyAttestation(env, "jwt", keyAttestationJwt));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
		assertEquals("Key attestation x5c certificate chain validation failed",
			env.getString("vci", "credential_error_response.body.error_description"));
	}

	private String createKeyAttestationJwt(List<Base64> x5cChain) throws Exception {
		ECKey signingKey = new ECKeyGenerator(Curve.P_256)
			.keyID("key-attestation-signing-key")
			.generate();

		Instant now = Instant.now();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plusSeconds(300)))
			.claim("attested_keys", List.of(signingKey.toPublicJWK().toJSONObject()))
			.build();

		JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("key-attestation+jwt"))
			.keyID(signingKey.getKeyID());
		if (x5cChain != null) {
			headerBuilder.x509CertChain(x5cChain);
		}

		SignedJWT jwt = new SignedJWT(headerBuilder.build(), claimsSet);
		jwt.sign(new ECDSASigner(signingKey));
		return jwt.serialize();
	}
}
