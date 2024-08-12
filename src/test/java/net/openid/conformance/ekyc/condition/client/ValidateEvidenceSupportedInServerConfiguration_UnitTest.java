package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateEvidenceSupportedInServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateEvidenceSupportedInServerConfiguration cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateEvidenceSupportedInServerConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"foo\""
			+ "]}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_empty () {
		// zero entries is explicitly permitted in https://openid.net/specs/openid-connect-4-identity-assurance-1_0-ID3.html#name-op-metadata
		JsonObject server = JsonParser.parseString("{"
			+ "\"evidence_supported\": ["
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missing () {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "}")
				.getAsJsonObject();
			env.putObject("server", server);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_notString() {
		assertThrows(ConditionError.class, () -> {
			env.putObjectFromJsonString("server", "{"
				+ "\"evidence_supported\": ["
				+ "\"foo\", false"
				+ "]}");
			cond.execute(env);
		});
	}

}
