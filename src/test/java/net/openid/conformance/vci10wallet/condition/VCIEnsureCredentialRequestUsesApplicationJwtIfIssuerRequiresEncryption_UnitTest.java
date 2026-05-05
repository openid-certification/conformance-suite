package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
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
public class VCIEnsureCredentialRequestUsesApplicationJwtIfIssuerRequiresEncryption_UnitTest {

	private VCIEnsureCredentialRequestUsesApplicationJwtIfIssuerRequiresEncryption cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialRequestUsesApplicationJwtIfIssuerRequiresEncryption();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void passesWhenIssuerDoesNotRequireEncryption() {
		setIssuerRequestEncryptionRequired(false);
		setIncomingContentType("application/json");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenIssuerRequiresEncryptionAndRequestIsEncrypted() {
		setIssuerRequestEncryptionRequired(true);
		setIncomingContentType("application/jwt");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenIssuerRequiresEncryptionAndRequestIsPlaintext() {
		setIssuerRequestEncryptionRequired(true);
		setIncomingContentType("application/json");
		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("Issuer requires credential request encryption"),
			"expected message to mention that the issuer requires encryption but was: " + err.getMessage());
	}

	@Test
	public void failsWhenIssuerRequiresEncryptionAndContentTypeMissing() {
		setIssuerRequestEncryptionRequired(true);
		setIncomingContentType(null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void setIncomingContentType(String contentType) {
		JsonObject headers = new JsonObject();
		if (contentType != null) {
			headers.addProperty("content-type", contentType);
		}
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("headers", headers);
		env.putObject("incoming_request", incomingRequest);
	}

	private void setIssuerRequestEncryptionRequired(boolean required) {
		JsonObject requestEnc = new JsonObject();
		requestEnc.addProperty("encryption_required", required);
		JsonObject metadata = new JsonObject();
		metadata.add("credential_request_encryption", requestEnc);
		env.putObject("credential_issuer_metadata", metadata);
	}
}
