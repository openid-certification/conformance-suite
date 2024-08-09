package net.openid.conformance.condition.client;

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
public class CheckTLSClientCertificateBoundAccessTokensTrue_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTLSClientCertificateBoundAccessTokensTrue cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckTLSClientCertificateBoundAccessTokensTrue();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseNull() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("server", new JsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseJson() {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{\"tls_client_certificate_bound_access_tokens\":{\"value\": true}}").getAsJsonObject();
			env.putObject("server", server);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseString() {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{\"tls_client_certificate_bound_access_tokens\":\"true\"}").getAsJsonObject();
			env.putObject("server", server);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject server = JsonParser.parseString("{\"tls_client_certificate_bound_access_tokens\":true}").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);
	}
}
