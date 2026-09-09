package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
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
 * Covers the two conditions on the SET {@code typ} header: the FAILURE-level media-type check
 * (SSF 1.0 4.1.1 explicit typing) and the WARNING-level preferred-spelling check (RFC 8417 2.3).
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFSecurityEventTokenType_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt typeCheck() {
		var condition = new OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private OIDSSFWarnSecurityEventTokenTypeNotInPreferredForm spellingCheck() {
		var condition = new OIDSSFWarnSecurityEventTokenTypeNotInPreferredForm();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		return condition;
	}

	private void prepareSetToken(String typ) {
		JsonObject header = new JsonObject();
		if (typ != null) {
			header.addProperty("typ", typ);
		}
		JsonObject setToken = new JsonObject();
		setToken.add("header", header);
		env.putObject("set_token", setToken);
	}

	@Test
	void preferredFormPassesBothChecks() {
		prepareSetToken("secevent+jwt");
		assertDoesNotThrow(() -> typeCheck().execute(env));
		assertDoesNotThrow(() -> spellingCheck().execute(env));
	}

	@Test
	void fullMediaTypeIsExplicitTypingButNotThePreferredSpelling() {
		// RFC 7515 4.1.9: a recipient treats a typ without '/' as if "application/" were
		// prepended, so the full form names the same media type; RFC 8417 2.3 only SHOULDs
		// the short form
		prepareSetToken("application/secevent+jwt");
		assertDoesNotThrow(() -> typeCheck().execute(env));
		assertThrows(ConditionError.class, () -> spellingCheck().execute(env));
	}

	@Test
	void differentCaseIsExplicitTypingButNotThePreferredSpelling() {
		// RFC 7515 4.1.9 / RFC 2045: media type values are case insensitive
		prepareSetToken("SecEvent+JWT");
		assertDoesNotThrow(() -> typeCheck().execute(env));
		assertThrows(ConditionError.class, () -> spellingCheck().execute(env));
	}

	@Test
	void otherMediaTypeFailsTheTypeCheck() {
		prepareSetToken("jwt");
		assertThrows(ConditionError.class, () -> typeCheck().execute(env));
	}

	@Test
	void otherApplicationMediaTypeFailsTheTypeCheck() {
		prepareSetToken("application/jwt");
		assertThrows(ConditionError.class, () -> typeCheck().execute(env));
	}

	@Test
	void missingTypFailsTheTypeCheckOnly() {
		// the missing header is the FAILURE-level finding; the spelling check has nothing to add
		prepareSetToken(null);
		assertThrows(ConditionError.class, () -> typeCheck().execute(env));
		assertDoesNotThrow(() -> spellingCheck().execute(env));
	}

	@Test
	void otherMediaTypeIsNotASpellingFinding() {
		// a wrong media type is reported by the type check; the spelling check stays silent
		prepareSetToken("jwt");
		assertDoesNotThrow(() -> spellingCheck().execute(env));
	}
}
