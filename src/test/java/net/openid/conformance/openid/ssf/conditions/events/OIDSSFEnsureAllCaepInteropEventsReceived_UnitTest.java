package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureAllCaepInteropEventsReceived_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDSSFEnsureAllCaepInteropEventsReceived createCondition(Set<String> expected, Set<String> received) {
		OIDSSFEnsureAllCaepInteropEventsReceived condition = new OIDSSFEnsureAllCaepInteropEventsReceived(expected, received);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return condition;
	}

	private OIDSSFEnsureAllCaepInteropEventsReceived createCondition(Set<String> received) {
		return createCondition(SsfEvents.CAEP_INTEROP_EVENT_TYPES, received);
	}

	@Test
	void shouldPassWhenAllThreeEventsReceived() {
		Set<String> received = new LinkedHashSet<>(Set.of(
			SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE,
			SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE,
			SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE
		));
		assertDoesNotThrow(() -> createCondition(received).execute(env));
	}

	@Test
	void shouldPassWhenExtraEventsReceived() {
		Set<String> received = new LinkedHashSet<>(Set.of(
			SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE,
			SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE,
			SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE,
			SsfEvents.CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE
		));
		assertDoesNotThrow(() -> createCondition(received).execute(env));
	}

	@Test
	void shouldFailWhenEmpty() {
		assertThrows(ConditionError.class, () -> createCondition(Set.of()).execute(env));
	}

	@Test
	void shouldFailWhenMissingSessionRevoked() {
		Set<String> received = new LinkedHashSet<>(Set.of(
			SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE,
			SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE
		));
		assertThrows(ConditionError.class, () -> createCondition(received).execute(env));
	}

	@Test
	void shouldFailWhenMissingCredentialChange() {
		Set<String> received = new LinkedHashSet<>(Set.of(
			SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE,
			SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE
		));
		assertThrows(ConditionError.class, () -> createCondition(received).execute(env));
	}

	@Test
	void shouldFailWhenMissingDeviceComplianceChange() {
		Set<String> received = new LinkedHashSet<>(Set.of(
			SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE,
			SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE
		));
		assertThrows(ConditionError.class, () -> createCondition(received).execute(env));
	}

	@Test
	void shouldPassWhenTransmitterSupportsOnlyOneEventAndItIsReceived() {
		Set<String> expected = Set.of(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		Set<String> received = Set.of(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		assertDoesNotThrow(() -> createCondition(expected, received).execute(env));
	}

	@Test
	void shouldFailWhenTransmitterSupportsOneEventButNotReceived() {
		Set<String> expected = Set.of(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		assertThrows(ConditionError.class, () -> createCondition(expected, Set.of()).execute(env));
	}

	@Test
	void shouldFailWhenOnlyUnrelatedEventsReceived() {
		Set<String> received = new LinkedHashSet<>(Set.of(
			SsfEvents.CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE,
			SsfEvents.CAEP_ASSURANCE_LEVEL_CHANGE_EVENT_TYPE
		));
		assertThrows(ConditionError.class, () -> createCondition(received).execute(env));
	}
}
