package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureDeliveryMethodIsSupported_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureDeliveryMethodIsSupported createCondition(SsfDeliveryMode deliveryMode) {
		OIDSSFEnsureDeliveryMethodIsSupported condition = new OIDSSFEnsureDeliveryMethodIsSupported(deliveryMode);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareMetadata(String metadataJson) {
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", JsonParser.parseString(metadataJson));
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWhenSelectedMethodIsAdvertised() {
		prepareMetadata("{\"delivery_methods_supported\":[\"urn:ietf:rfc:8935\",\"https://schemas.openid.net/secevent/risc/delivery-method/push\"]}");
		assertDoesNotThrow(() -> createCondition(SsfDeliveryMode.PUSH).execute(env));
	}

	@Test
	void failsWhenSelectedMethodIsNotAdvertised() {
		// e.g. a push-only transmitter scheduled for a POLL certification run
		prepareMetadata("{\"delivery_methods_supported\":[\"urn:ietf:rfc:8935\"]}");
		assertThrows(ConditionError.class, () -> createCondition(SsfDeliveryMode.POLL).execute(env));
	}

	@Test
	void failsInsteadOfCrashingWhenFieldIsMissing() {
		prepareMetadata("{\"issuer\":\"https://transmitter.example\"}");
		assertThrows(ConditionError.class, () -> createCondition(SsfDeliveryMode.PUSH).execute(env));
	}

	@Test
	void failsInsteadOfCrashingWhenFieldIsNotAnArray() {
		prepareMetadata("{\"delivery_methods_supported\":\"urn:ietf:rfc:8935\"}");
		assertThrows(ConditionError.class, () -> createCondition(SsfDeliveryMode.PUSH).execute(env));
	}
}
