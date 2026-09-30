package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnsureHttpResponseBodyIsEmpty_UnitTest {

	private void checkBody(String body) {
		Environment env = new Environment();
		JsonObject response = new JsonObject();
		response.addProperty("status", 202);
		response.addProperty("body", body);
		env.putObject("endpoint_response", response);
		var condition = new EnsureHttpResponseBodyIsEmpty();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
		condition.execute(env);
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void acceptsEmptyResponse(String body) {
		assertDoesNotThrow(() -> checkBody(body));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "processing", " "})
	public void rejectsNonEmptyResponse(String body) {
		assertThrows(ConditionError.class, () -> checkBody(body));
	}
}
