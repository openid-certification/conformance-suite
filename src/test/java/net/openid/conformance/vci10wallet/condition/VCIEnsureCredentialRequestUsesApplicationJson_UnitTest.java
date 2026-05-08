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

@ExtendWith(MockitoExtension.class)
public class VCIEnsureCredentialRequestUsesApplicationJson_UnitTest {

	private VCIEnsureCredentialRequestUsesApplicationJson cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialRequestUsesApplicationJson();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void passesForApplicationJson() {
		setIncomingContentType("application/json");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesForApplicationJsonWithCharsetParameter() {
		setIncomingContentType("application/json; charset=utf-8");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsForApplicationJwt() {
		setIncomingContentType("application/jwt");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsForTextPlain() {
		setIncomingContentType("text/plain");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsForMissingContentType() {
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
}
