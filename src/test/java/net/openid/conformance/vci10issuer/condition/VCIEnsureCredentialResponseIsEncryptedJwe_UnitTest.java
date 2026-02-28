package net.openid.conformance.vci10issuer.condition;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureCredentialResponseIsEncryptedJwe_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VCIEnsureCredentialResponseIsEncryptedJwe cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialResponseIsEncryptedJwe();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setResponseBody(String body) {
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.addProperty("body", body);
		env.putObject("endpoint_response", endpointResponse);
	}

	@Test
	public void testValidJwe() {
		// A JWE compact serialization has 5 base64url parts separated by dots
		setResponseBody("eyJhbGciOiJSU0EtT0FFUCJ9.dGVzdA.iv_part.ciphertext_part.tag_part");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testValidJweWithEmptyEncryptedKey() {
		// JWE can have an empty second part (e.g. for direct key agreement)
		setResponseBody("eyJhbGciOiJFQ0RILUVTIn0..iv_part.ciphertext_part.tag_part");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testRejectsJsonErrorResponse() {
		// This is the bug case: a JSON error response with enough dots to split into 5 parts
		setResponseBody("{\"error\":\"invalid_nonce\",\"error_description\":\"[A378319] proofs.jwt[0]: " +
			"The value of the 'nonce' claim in the JWT key proof does not match the expected value " +
			"('1Iptp8kes3oGnKkX2HUAtVanw_58V_AHqqYmjJtKC9c').\",\"error_uri\":" +
			"\"https://docs.authlete.com/#A378319\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testRejectsJsonSuccessResponse() {
		setResponseBody("{\"credential\":\"eyJ0eXAi.eyJzdWIi.sig\"}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testRejectsEmptyString() {
		setResponseBody("");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testRejectsThreePartJwt() {
		// A JWS/JWT has 3 parts, not 5
		setResponseBody("eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIxMjM0In0.signature");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
