package net.openid.conformance.openid.ssf.conditions.subjects;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnCaepInteropComplexSubjectsConfigured_UnitTest {

	private static final String EMAIL = "{\"format\":\"email\",\"email\":\"foo@example.com\"}";

	private static final String COMPLEX = """
		{"format":"complex",
		 "user":{"format":"email","email":"bar@example.com"},
		 "tenant":{"format":"iss_sub","iss":"https://example.com/idp1","sub":"1234"}}""";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnCaepInteropComplexSubjectsConfigured condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFWarnCaepInteropComplexSubjectsConfigured();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void setUp(String validJsonArray) {
		JsonObject ssf = new JsonObject();
		if (validJsonArray != null) {
			ssf.add("event_subjects", JsonParser.parseString(validJsonArray));
		}
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWithoutComplexSubjects() {
		setUp("[" + EMAIL + "]");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWhenNoSubjectsResolved() {
		setUp(null);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldWarnForComplexSubject() {
		setUp("[" + EMAIL + "," + COMPLEX + "]");
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("'SSF valid SubjectId' test configuration declares 1 Complex Subject(s)"));
		assertTrue(e.getMessage().contains("sharedsignals#351"));
	}
}
