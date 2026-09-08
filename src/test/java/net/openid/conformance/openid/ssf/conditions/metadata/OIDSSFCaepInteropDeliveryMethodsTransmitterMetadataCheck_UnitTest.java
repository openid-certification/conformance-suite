package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the CAEPIOP 2.3.2 presence check. Whether the advertised methods include the
 * scheduled run's delivery mode is judged separately by
 * {@link net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFEnsureDeliveryMethodIsSupported}.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFCaepInteropDeliveryMethodsTransmitterMetadataCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFCaepInteropDeliveryMethodsTransmitterMetadataCheck createCondition() {
		OIDSSFCaepInteropDeliveryMethodsTransmitterMetadataCheck condition = new OIDSSFCaepInteropDeliveryMethodsTransmitterMetadataCheck();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareMetadata(String metadataJson) {
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", JsonParser.parseString(metadataJson));
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWhenBothStandardMethodsAreAdvertised() {
		prepareMetadata("{\"delivery_methods_supported\":[\"urn:ietf:rfc:8935\",\"urn:ietf:rfc:8936\"]}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWhenOnlyOneStandardMethodIsAdvertised() {
		// presence only - a push-only transmitter (e.g. advertising the RFC 8935 URI plus a
		// legacy RISC push URI) satisfies CAEPIOP 2.3.2; the scheduled run's delivery mode
		// is checked separately by OIDSSFEnsureDeliveryMethodIsSupported
		prepareMetadata("{\"delivery_methods_supported\":[\"urn:ietf:rfc:8935\",\"https://schemas.openid.net/secevent/risc/delivery-method/push\"]}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenFieldIsMissing() {
		prepareMetadata("{\"issuer\":\"https://transmitter.example\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenFieldIsNotAnArray() {
		prepareMetadata("{\"delivery_methods_supported\":\"urn:ietf:rfc:8935\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenFieldIsEmpty() {
		prepareMetadata("{\"delivery_methods_supported\":[]}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
