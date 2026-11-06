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
public class CheckForUnexpectedFieldsInPoppyClientMetadataDocument_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedFieldsInPoppyClientMetadataDocument cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckForUnexpectedFieldsInPoppyClientMetadataDocument();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(Consumer<JsonObject> mutate) {
		JsonObject doc = PoppyTestData.json(PoppyTestData.CLIENT_METADATA);
		mutate.accept(doc);
		env.putObject("client", doc);
		cond.execute(env);
	}

	@Test
	public void specExamplePasses() {
		assertDoesNotThrow(() -> run(d -> { }));
	}

	@Test
	public void rfc7591NamesPass() {
		assertDoesNotThrow(() -> run(d -> {
			d.addProperty("client_uri", "https://agent.example");
			d.addProperty("tos_uri", "https://agent.example/tos");
			d.addProperty("software_id", "x");
			d.addProperty("client_name#de", "Beispiel");
		}));
	}

	@Test
	public void unknownFieldFails() {
		assertThrows(ConditionError.class, () -> run(d -> d.addProperty("client_colour", "red")));
	}
}
