package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AddRandomFieldsToJsonObject_UnitTest {

	private static final String TARGET_DESCRIPTION = "test target";
	private static final String ENV_KEY = "example_request";

	@Mock
	private TestInstanceEventLog eventLog;

	private Environment env;

	@BeforeEach
	public void setUp() {
		env = new Environment();
	}

	@Test
	public void testEvaluate_addsRandomField() {
		JsonObject target = new JsonObject();
		target.addProperty("existing", "value");
		env.putObject(ENV_KEY, target);

		Set<String> originalKeys = Set.copyOf(target.keySet());

		AddRandomFieldsToJsonObject cond = new AddRandomFieldsToJsonObject(TARGET_DESCRIPTION, ENV_KEY);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		cond.execute(env);

		// The same JsonObject reference was mutated in place, and exactly one new property was added.
		assertSame(target, env.getObject(ENV_KEY));
		assertEquals(originalKeys.size() + 1, target.keySet().size());
		assertEquals("value", OIDFJSON.getString(target.get("existing")));

		String addedKey = target.keySet().stream()
			.filter(k -> !originalKeys.contains(k))
			.findFirst()
			.orElseThrow();
		assertEquals(16, addedKey.length());
		String addedValue = OIDFJSON.getString(target.get(addedKey));
		assertEquals(16, addedValue.length());
	}

	@Test
	public void testEvaluate_throwsWhenEnvKeyMissing() {
		AddRandomFieldsToJsonObject cond = new AddRandomFieldsToJsonObject(TARGET_DESCRIPTION, ENV_KEY);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);

		ConditionError err = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertNotNull(err.getMessage());
		assertTrue(err.getMessage().contains("not found"),
			"Expected error message to mention that the target was not found, got: " + err.getMessage());
	}
}
