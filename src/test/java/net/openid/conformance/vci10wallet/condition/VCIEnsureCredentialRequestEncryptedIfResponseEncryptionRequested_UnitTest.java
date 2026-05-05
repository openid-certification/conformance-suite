package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureCredentialRequestEncryptedIfResponseEncryptionRequested_UnitTest {

	private VCIEnsureCredentialRequestEncryptedIfResponseEncryptionRequested cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialRequestEncryptedIfResponseEncryptionRequested();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void passesWhenBodyDoesNotRequestResponseEncryption() {
		putIncomingRequest("application/json", "{\"credential_identifier\":\"X\"}");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenBodyRequestsResponseEncryptionAndRequestIsEncrypted() {
		putIncomingRequest("application/jwt",
			"{\"credential_response_encryption\":{\"jwk\":{},\"enc\":\"A256GCM\"}}");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenBodyRequestsResponseEncryptionAndRequestIsPlaintext() {
		putIncomingRequest("application/json",
			"{\"credential_response_encryption\":{\"jwk\":{},\"enc\":\"A256GCM\"}}");
		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("credential_response_encryption"),
			"expected message to mention credential_response_encryption but was: " + err.getMessage());
	}

	@Test
	public void failsWhenBodyRequestsResponseEncryptionAndContentTypeMissing() {
		putIncomingRequest(null,
			"{\"credential_response_encryption\":{\"jwk\":{},\"enc\":\"A256GCM\"}}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void passesWhenRequestIsEncryptedAndBodyJsonIsAbsent() {
		// Realistic case: a real encrypted request has a JWE compact serialization in `body`
		// (not JSON), so `body_json` is not populated. The condition should still pass purely
		// on the basis that the request is encrypted — § 8.2-18 is satisfied by the encryption.
		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/jwt");
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("headers", headers);
		incomingRequest.addProperty("body", "eyJhbGc.dummy.jwe");
		// deliberately do not set body_json
		env.putObject("incoming_request", incomingRequest);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	private void putIncomingRequest(String contentType, String body) {
		JsonObject headers = new JsonObject();
		if (contentType != null) {
			headers.addProperty("content-type", contentType);
		}
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("headers", headers);
		incomingRequest.addProperty("body", body);
		incomingRequest.add("body_json", JsonParser.parseString(body));
		env.putObject("incoming_request", incomingRequest);
	}
}
