package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
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

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsurePushDeliveryResponseBodyIsEmpty_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsurePushDeliveryResponseBodyIsEmpty condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFEnsurePushDeliveryResponseBodyIsEmpty();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void pushResponse(String body) {
		JsonObject response = new JsonObject();
		response.addProperty("status", 202);
		response.addProperty("endpoint_name", "receiver push endpoint");
		response.addProperty("body", body);
		env.putObject("endpoint_response", response);
	}

	@Test
	void passesWhenTheBodyIsAbsent() {
		JsonObject response = new JsonObject();
		response.addProperty("status", 202);
		env.putObject("endpoint_response", response);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWhenTheBodyIsNull() {
		pushResponse(null);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWhenTheBodyIsEmpty() {
		pushResponse("");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWhenTheBodyIsWhitespaceOnly() {
		pushResponse(" \n\t");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsWhenTheBodyCarriesContent() {
		pushResponse("{\"status\":\"accepted\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenTheResponseIsMissing() {
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
