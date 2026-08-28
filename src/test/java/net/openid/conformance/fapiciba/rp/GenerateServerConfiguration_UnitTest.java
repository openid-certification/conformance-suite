package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class GenerateServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private GenerateServerConfiguration condition;

	@BeforeEach
	public void setup() {
		condition = new GenerateServerConfiguration();
		condition.setProperties("test", eventLog, Condition.ConditionResult.INFO);
		env.putString("base_url", "https://example.com/test/a/alias");
	}

	@Test
	public void testEvaluate_publishesSubjectTypesSupported() {
		condition.evaluate(env);

		JsonObject server = env.getObject("server");
		JsonArray subjectTypes = server.getAsJsonArray("subject_types_supported");
		assertNotNull(subjectTypes, "subject_types_supported is REQUIRED provider metadata");
		assertEquals(List.of("public"), OIDFJSON.convertJsonArrayToList(subjectTypes));
	}

	@Test
	public void testEvaluate_publishedScopesSupportedContainsOpenId() {
		condition.evaluate(env);

		JsonObject server = env.getObject("server");
		JsonArray scopes = server.getAsJsonArray("scopes_supported");
		assertNotNull(scopes, "scopes_supported is not published");
		List<String> scopeList = OIDFJSON.convertJsonArrayToList(scopes);
		assertTrue(scopeList.contains("openid"), "scopes_supported must contain openid");
		assertTrue(scopeList.contains("accounts"), "scopes_supported must contain accounts, requested by the CI self-test clients");
	}
}
