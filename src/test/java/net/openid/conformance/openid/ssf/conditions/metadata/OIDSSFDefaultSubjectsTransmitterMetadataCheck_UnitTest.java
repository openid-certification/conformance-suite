package net.openid.conformance.openid.ssf.conditions.metadata;

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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFDefaultSubjectsTransmitterMetadataCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFDefaultSubjectsTransmitterMetadataCheck condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFDefaultSubjectsTransmitterMetadataCheck();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setMetadata(String json) {
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", JsonParser.parseString(json));
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWhenAbsent() {
		setMetadata("{\"issuer\":\"https://transmitter.example\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForAll() {
		setMetadata("{\"default_subjects\":\"ALL\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForNone() {
		setMetadata("{\"default_subjects\":\"NONE\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsForLowerCase() {
		setMetadata("{\"default_subjects\":\"all\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForOtherValue() {
		setMetadata("{\"default_subjects\":\"SOME\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForNonString() {
		setMetadata("{\"default_subjects\":true}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
