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
public class OIDSSFCaepInteropAuthorizationSchemesTransmitterMetadataCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFCaepInteropAuthorizationSchemesTransmitterMetadataCheck condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFCaepInteropAuthorizationSchemesTransmitterMetadataCheck();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setMetadata(String json) {
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", JsonParser.parseString(json));
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWithRfc6749Scheme() {
		setMetadata("{\"authorization_schemes\":[{\"spec_urn\":\"urn:ietf:rfc:6749\"}]}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWithAdditionalWellFormedSchemes() {
		setMetadata("{\"authorization_schemes\":[{\"spec_urn\":\"urn:ietf:rfc:6749\"},{\"spec_urn\":\"urn:example:custom\"}]}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsWhenAbsent() {
		setMetadata("{\"issuer\":\"https://transmitter.example\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenNotAnArray() {
		setMetadata("{\"authorization_schemes\":{\"spec_urn\":\"urn:ietf:rfc:6749\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWithoutRfc6749Scheme() {
		setMetadata("{\"authorization_schemes\":[{\"spec_urn\":\"urn:example:custom\"}]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenAnEntryAfterRfc6749LacksSpecUrn() {
		setMetadata("{\"authorization_schemes\":[{\"spec_urn\":\"urn:ietf:rfc:6749\"},{\"name\":\"custom\"}]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenAnEntryAfterRfc6749HasANonUrnSpecUrn() {
		setMetadata("{\"authorization_schemes\":[{\"spec_urn\":\"urn:ietf:rfc:6749\"},{\"spec_urn\":\"https://example.com/scheme\"}]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenAnEntryIsNotAnObject() {
		setMetadata("{\"authorization_schemes\":[\"urn:ietf:rfc:6749\"]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenSpecUrnIsNotAString() {
		setMetadata("{\"authorization_schemes\":[{\"spec_urn\":6749}]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
