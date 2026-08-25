package net.openid.conformance.openid.ssf.conditions.events;

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

import static net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureCaepInteropEventSubjectFormat_UnitTest.setUpCaepEvent;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnCaepInteropEventUsesComplexSubject_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnCaepInteropEventUsesComplexSubject condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFWarnCaepInteropEventUsesComplexSubject();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	@Test
	void shouldPassWithSimpleSubject() {
		setUpCaepEvent(env, JsonParser.parseString("{\"format\":\"email\",\"email\":\"user@example.com\"}"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWhenSubIdIsMissing() {
		// presence is checked by OIDSSFValidateSecurityEventTokenSubIdClaim
		setUpCaepEvent(env, null);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldWarnWithComplexSubjectAndReferenceWorkingGroupIssue() {
		setUpCaepEvent(env, JsonParser.parseString("""
			{"format":"complex",
			 "user":{"format":"email","email":"user@example.com"},
			 "device":{"format":"opaque","id":"device-1"}}"""));
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("sharedsignals#351"));
		assertFalse(e.getMessage().contains("none of its Subject Members"));
	}

	@Test
	void shouldNotFlagStandardMemberNames() {
		setUpCaepEvent(env, JsonParser.parseString("""
			{"format":"complex",
			 "user":{"format":"email","email":"user@example.com"},
			 "device":{"format":"opaque","id":"device-1"},
			 "tenant":{"format":"opaque","id":"tenant-1"}}"""));
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertFalse(e.getMessage().contains("not among those defined"));
	}

	@Test
	void shouldFlagNonStandardMemberNames() {
		setUpCaepEvent(env, JsonParser.parseString("""
			{"format":"complex",
			 "user":{"format":"email","email":"user@example.com"},
			 "devcie":{"format":"opaque","id":"device-1"}}"""));
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("[devcie]"));
		assertTrue(e.getMessage().contains("not among those defined in SSF 1.0 section 3.3"));
	}

	@Test
	void shouldMentionMissingProfileSupportedMemberForOpaqueOnlyComplexSubject() {
		setUpCaepEvent(env, JsonParser.parseString("""
			{"format":"complex",
			 "device":{"format":"opaque","id":"device-1"},
			 "tenant":{"format":"opaque","id":"tenant-1"}}"""));
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("none of its Subject Members"));
	}
}
