package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateKeyAttestationX5cCertificateChain_UnitTest {

	private static final String SELF_SIGNED_CERT = "MIIBkjCCATegAwIBAgIUZkRih1mNAs9PfQphhjLx8O2Uej8wCgYIKoZIzj0EAwIwHTEbMBkGA1UEAwwSeDV0LXMyNTYtdW5pdC10ZXN0MCAXDTI2MDIwODE1NTEwMVoYDzIxMjYwMTE1MTU1MTAxWjAdMRswGQYDVQQDDBJ4NXQtczI1Ni11bml0LXRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQx5OCxLnQFHvYuP74zEU9MvsM0rEKULKZ2qjWFnz/T1eXB8JKRu4i77bKgONYDaMHQLeEaPN73RPj+nlhpnoC3o1MwUTAdBgNVHQ4EFgQUQYMPimHGw8fD+nAw5hXN1tLeHE8wHwYDVR0jBBgwFoAUQYMPimHGw8fD+nAw5hXN1tLeHE8wDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEAgyNkETTSsp/nkhXKjNETK4UGQXSayRAFtZ6hJSyKIOUCIQCIW7UskVfn6zliot/KzfmqY1XDjaTf6kzqhv5YBlRmtg==";

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateKeyAttestationX5cCertificateChain cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateKeyAttestationX5cCertificateChain();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_setsInvalidProofErrorResponseWhenChainValidationFails() {
		JsonArray x5c = new JsonArray();
		x5c.add(SELF_SIGNED_CERT);

		JsonObject header = new JsonObject();
		header.add("x5c", x5c);

		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.add("header", header);

		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
		assertEquals("Key attestation x5c certificate chain validation failed",
			env.getString("vci", "credential_error_response.body.error_description"));
	}
}
