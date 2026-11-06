package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedFieldsInPoppyJson_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedFieldsInPoppyJson cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckForUnexpectedFieldsInPoppyJson();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(Consumer<JsonObject> mutate) {
		JsonObject json = PoppyTestData.json(PoppyTestData.POPPY_JSON);
		mutate.accept(json);
		env.putObject("poppy_json", json);
		cond.execute(env);
	}

	@Test
	public void examplePasses() {
		assertDoesNotThrow(() -> run(j -> { }));
	}

	@Test
	public void skillIndexPasses() {
		assertDoesNotThrow(() -> run(j -> j.add("skill_index", new JsonObject())));
	}

	@Test
	public void unknownTopLevelFails() {
		assertThrows(ConditionError.class, () -> run(j -> j.addProperty("foo", "bar")));
	}

	@Test
	public void misspeltSkillsFails() {
		assertThrows(ConditionError.class, () -> run(j -> j.add("skills", new JsonObject())));
	}

	@Test
	public void unknownOrganizationFieldFails() {
		assertThrows(ConditionError.class, () -> run(j -> j.getAsJsonObject("organization").addProperty("colour", "red")));
	}

	@Test
	public void unknownApiFieldFails() {
		assertThrows(ConditionError.class, () -> run(j -> j.getAsJsonArray("apis").get(0).getAsJsonObject().addProperty("foo", 1)));
	}

	@Test
	public void unknownAgentProtocolFieldFails() {
		assertThrows(ConditionError.class, () -> run(j -> j.getAsJsonObject("agent").getAsJsonArray("protocols").get(0).getAsJsonObject().addProperty("foo", 1)));
	}
}
