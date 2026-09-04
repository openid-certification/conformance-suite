package net.openid.conformance.openid.ssf.conditions.metadata;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFSpecVersionTransmitterMetadataCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFSpecVersionTransmitterMetadataCheck check;

	@BeforeEach
	public void setUp() {
		check = new OIDSSFSpecVersionTransmitterMetadataCheck();
		check.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	// isValidVersion tests

	@Test
	void versionNullShouldBeNotValid() {
		assertFalse(check.isValidVersion(null));
	}

	@Test
	void versionEmptyShouldBeNotValid() {
		assertFalse(check.isValidVersion(""));
	}

	@Test
	void version1dot0ShouldBeNotValid() {
		assertFalse(check.isValidVersion("1.0"));
	}

	@Test
	void version1_0ID1ShouldBeNotValid() {
		assertFalse(check.isValidVersion("1_0-ID1"));
	}

	@Test
	void version1_0ID2ShouldBeNotValid() {
		// CAEP Interop Profile 2.3.1 (draft-01 and later): "MUST be 1_0 or greater" -
		// an implementer's draft of 1_0 precedes 1_0 itself.
		assertFalse(check.isValidVersion("1_0-ID2"));
	}

	@Test
	void version1_0ID3ShouldBeNotValid() {
		assertFalse(check.isValidVersion("1_0-ID3"));
	}

	@Test
	void version1_0ID10ShouldBeNotValid() {
		assertFalse(check.isValidVersion("1_0-ID10"));
	}

	@Test
	void nonNumericVersionShouldBeNotValidWithoutThrowing() {
		assertFalse(check.isValidVersion("abc"));
		assertFalse(check.isValidVersion("1_x"));
		assertFalse(check.isValidVersion("1"));
		assertFalse(check.isValidVersion("-ID2"));
	}

	@Test
	void degenerateFormsShouldBeNotValid() {
		assertFalse(check.isValidVersion("1_0-"));
		assertFalse(check.isValidVersion("01_0"));
		assertFalse(check.isValidVersion("1_00"));
	}

	@Test
	void version1_0ShouldBeValid() {
		assertTrue(check.isValidVersion("1_0"));
	}

	@Test
	void version1_1ShouldBeValid() {
		assertTrue(check.isValidVersion("1_1"));
	}

	@Test
	void version2_0ID1ShouldBeValid() {
		// a draft of a version beyond 1_0 still satisfies "1_0 or greater"
		assertTrue(check.isValidVersion("2_0-ID1"));
	}

	// evaluate() tests

	@Test
	void evaluateShouldFailWhenSpecVersionIsMissing() {
		JsonObject transmitterMetadata = new JsonObject();
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", transmitterMetadata);
		env.putObject("ssf", ssf);

		assertThrows(ConditionError.class, () -> check.execute(env));
	}

	@Test
	void evaluateShouldFailWhenSpecVersionIsInvalid() {
		JsonObject transmitterMetadata = new JsonObject();
		transmitterMetadata.addProperty("spec_version", "1_0-ID1");
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", transmitterMetadata);
		env.putObject("ssf", ssf);

		assertThrows(ConditionError.class, () -> check.execute(env));
	}

	@Test
	void evaluateShouldSucceedWhenSpecVersionIsValid() {
		JsonObject transmitterMetadata = new JsonObject();
		transmitterMetadata.addProperty("spec_version", "1_0");
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", transmitterMetadata);
		env.putObject("ssf", ssf);

		check.execute(env);
	}
}
