package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsurePidPictureClaimDisclosed_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePidPictureClaimDisclosed cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsurePidPictureClaimDisclosed();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	private void setupEnvironment(String decodedJson) {
		env.putObject("sdjwt", "decoded", JsonParser.parseString(decodedJson).getAsJsonObject());
	}

	@Test
	public void testEvaluate_picturePresentPasses() {
		setupEnvironment("""
			{
			  "given_name": "John",
			  "picture": "data:image/jpeg;base64,abc123"
			}
			""");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_pictureMissingFails() {
		setupEnvironment("""
			{
			  "given_name": "John"
			}
			""");

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noDecodedClaimsFails() {
		env.putObject("sdjwt", new JsonObject());

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}
}
