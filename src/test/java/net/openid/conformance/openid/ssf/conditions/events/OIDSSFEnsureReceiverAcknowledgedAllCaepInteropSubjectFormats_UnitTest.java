package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats condition(Map<String, String> formatByJti, Set<String> acked) {
		var condition = new OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats(formatByJti, acked);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	@Test
	void shouldPassWhenBothFormatsAcknowledged() {
		var condition = condition(
			Map.of("jti-1", "email", "jti-2", "iss_sub", "jti-3", "email", "jti-4", "iss_sub"),
			Set.of("jti-1", "jti-2", "jti-3", "jti-4"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWhenAtLeastOneEventPerFormatAcknowledged() {
		var condition = condition(
			Map.of("jti-1", "email", "jti-2", "iss_sub", "jti-3", "email"),
			Set.of("jti-1", "jti-2"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenOneFormatWasNeverAcknowledged() {
		var condition = condition(
			Map.of("jti-1", "email", "jti-2", "iss_sub"),
			Set.of("jti-1"));
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("subject identifier formats"));
	}

	@Test
	void shouldFailWhenNoEventsWereGenerated() {
		var condition = condition(Map.of(), Set.of());
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldIgnoreAcknowledgedJtisThatWereNotGeneratedAsCaepEvents() {
		// e.g. the verification event jti is acknowledged but has no subject format entry
		var condition = condition(
			Map.of("jti-1", "email"),
			Set.of("jti-1", "verification-jti"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
