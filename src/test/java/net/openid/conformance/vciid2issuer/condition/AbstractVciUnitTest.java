package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class AbstractVciUnitTest {

	protected Map<String, Object> assertValidationError(AbstractCondition condition, Environment env, TestInstanceEventLog eventLog) {

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class); // adjust type if needed

		Assertions.assertThrows(ConditionError.class, () -> {
			condition.evaluate(env);
		});

		verify(eventLog, times(1)).log(messageCaptor.capture(), mapCaptor.capture());
		Map<String, Object> data = mapCaptor.getValue();
		return data;
	}

	protected String readFile(String filePath) throws IOException {
		try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
			return new String(resourceAsStream.readAllBytes());
		}
	}

	protected void assertContainsExpectedError(Map<String, Object> data, String property, String expectedError) {
		@SuppressWarnings("unchecked")
		List<JsonObject> invalidEntries = (List<JsonObject>) data.get("invalid_entries");
		Assertions.assertFalse(invalidEntries.isEmpty());

		Map<String, String> propertyErrors = new HashMap<>();

		for (JsonObject invalidEntry : invalidEntries) {
			String propertyPath = OIDFJSON.getString(invalidEntry.get("path"));
			String propertyError = OIDFJSON.getString(invalidEntry.get("error"));
			propertyErrors.put(propertyPath, propertyError);
		}

		Assertions.assertEquals(expectedError, propertyErrors.get(property));
	}
}
