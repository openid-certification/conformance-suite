package net.openid.conformance.openid.ssf.conditions.subjects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFResolveEventSubjects_UnitTest {

	private static final String EMAIL_SUBJECT = "{\"format\":\"email\",\"email\":\"jane@example.com\"}";

	private static final String ISS_SUB_SUBJECT = "{\"format\":\"iss_sub\",\"iss\":\"https://idp.example.com\",\"sub\":\"jane\"}";

	private static final String OPAQUE_SUBJECT = "{\"format\":\"opaque\",\"id\":\"valid\"}";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFResolveEventSubjects condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFResolveEventSubjects();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setUp(SsfProfile profile, String subjectsConfigJson) {
		JsonObject config = new JsonObject();
		if (subjectsConfigJson != null) {
			JsonObject ssfConfig = new JsonObject();
			ssfConfig.add("subjects", JsonParser.parseString(subjectsConfigJson));
			config.add("ssf", ssfConfig);
		}
		env.putObject("config", config);

		JsonObject ssf = new JsonObject();
		ssf.addProperty("profile", profile.name());
		env.putObject("ssf", ssf);
	}

	private JsonArray resolved(String key) {
		JsonElement subjects = env.getElementFromObject("ssf", key);
		assertTrue(subjects != null && subjects.isJsonArray(), "expected ssf." + key + " to be an array");
		return subjects.getAsJsonArray();
	}

	private static List<String> formats(JsonArray subjects) {
		List<String> formats = new ArrayList<>();
		subjects.forEach(s -> formats.add(OIDFJSON.getString(s.getAsJsonObject().get("format"))));
		return formats;
	}

	// --- shared behaviour ---

	@Test
	void shouldAcceptSingleObjectForBackwardsCompatibility() {
		setUp(SsfProfile.DEFAULT, "{\"valid\":" + OPAQUE_SUBJECT + "}");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(List.of("opaque"), formats(resolved("event_subjects")));
	}

	@Test
	void shouldAcceptListOfSubjects() {
		setUp(SsfProfile.DEFAULT, "{\"valid\":[" + EMAIL_SUBJECT + "," + ISS_SUB_SUBJECT + "," + OPAQUE_SUBJECT + "]}");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(List.of("email", "iss_sub", "opaque"), formats(resolved("event_subjects")));
	}

	@Test
	void shouldIgnoreLegacyInvalidSubjectsField() {
		// 'ssf.subjects.invalid' existed in older configurations but is no longer used
		setUp(SsfProfile.DEFAULT, "{\"valid\":" + OPAQUE_SUBJECT + ",\"invalid\":{\"format\":\"email\",\"email\":\"not-an-email\"}}");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(List.of("opaque"), formats(resolved("event_subjects")));
	}

	@Test
	void shouldDropDuplicateEntries() {
		setUp(SsfProfile.DEFAULT, "{\"valid\":[" + EMAIL_SUBJECT + "," + EMAIL_SUBJECT + "]}");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(1, resolved("event_subjects").size());
	}

	@Test
	void shouldFailWhenValidSubjectsAreMissing() {
		setUp(SsfProfile.DEFAULT, null);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("'SSF valid SubjectId'"));
	}

	@Test
	void shouldFailWhenValidSubjectsAreEmptyList() {
		setUp(SsfProfile.DEFAULT, "{\"valid\":[]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenAValidSubjectIsMalformed() {
		setUp(SsfProfile.DEFAULT, "{\"valid\":[" + EMAIL_SUBJECT + ",{\"format\":\"iss_sub\",\"iss\":\"https://idp.example.com\"}]}");
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("'SSF valid SubjectId' field entry 2"));
		assertTrue(e.getMessage().contains("'sub'"));
	}

	@Test
	void shouldFailWhenValidSubjectIsNotAnObject() {
		setUp(SsfProfile.DEFAULT, "{\"valid\":\"jane@example.com\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	// --- CAEP interop profile ---

	@Test
	void caepInteropShouldUseEmailAndIssSubSubjects() {
		setUp(SsfProfile.CAEP_INTEROP, "{\"valid\":[" + EMAIL_SUBJECT + "," + ISS_SUB_SUBJECT + "]}");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(List.of("email", "iss_sub"), formats(resolved("event_subjects")));
	}

	@Test
	void caepInteropShouldKeepComplexSubjects() {
		String complex = """
			{"format":"complex",
			 "user":{"format":"email","email":"bar@example.com"},
			 "tenant":{"format":"iss_sub","iss":"https://example.com/idp1","sub":"1234"}}""";
		setUp(SsfProfile.CAEP_INTEROP, "{\"valid\":[" + EMAIL_SUBJECT + "," + ISS_SUB_SUBJECT + "," + complex + "]}");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(List.of("email", "iss_sub", "complex"), formats(resolved("event_subjects")));
	}

	@Test
	void caepInteropComplexSubjectDoesNotSatisfySimpleFormatCoverage() {
		String complex = "{\"format\":\"complex\",\"user\":" + EMAIL_SUBJECT + ",\"tenant\":" + ISS_SUB_SUBJECT + "}";
		setUp(SsfProfile.CAEP_INTEROP, "{\"valid\":[" + EMAIL_SUBJECT + "," + complex + "]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void caepInteropShouldSkipSubjectsInOtherFormats() {
		setUp(SsfProfile.CAEP_INTEROP, "{\"valid\":[" + OPAQUE_SUBJECT + "," + EMAIL_SUBJECT + "," + ISS_SUB_SUBJECT + "]}");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(List.of("email", "iss_sub"), formats(resolved("event_subjects")));
	}

	@Test
	void caepInteropShouldFailWhenIssSubIsMissing() {
		setUp(SsfProfile.CAEP_INTEROP, "{\"valid\":[" + EMAIL_SUBJECT + "]}");
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("'SSF valid SubjectId'"));
		assertTrue(e.getMessage().contains("section 2.5"));
	}

	@Test
	void caepInteropShouldFailWhenEmailIsMissing() {
		setUp(SsfProfile.CAEP_INTEROP, "{\"valid\":" + ISS_SUB_SUBJECT + "}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void caepInteropShouldFailWhenOnlyOpaqueSubjectIsConfigured() {
		// the pre-existing single-opaque configuration cannot be used for CAEP events (CAEPIOP-2.5)
		setUp(SsfProfile.CAEP_INTEROP, "{\"valid\":" + OPAQUE_SUBJECT + "}");
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("email"));
		assertTrue(e.getMessage().contains("iss_sub"));
	}
}
