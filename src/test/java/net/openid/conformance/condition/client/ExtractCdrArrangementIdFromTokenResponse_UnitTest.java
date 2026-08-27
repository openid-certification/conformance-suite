package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractCdrArrangementIdFromTokenResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractCdrArrangementIdFromTokenResponse cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractCdrArrangementIdFromTokenResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_present() {
		env.putObject("token_endpoint_response",
			JsonParser.parseString("{\"access_token\":\"at\",\"cdr_arrangement_id\":\"arrangement-123\"}").getAsJsonObject());

		cond.execute(env);

		assertEquals("arrangement-123", env.getString("cdr_arrangement_id"));
	}

	@Test
	public void testEvaluate_missing() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("token_endpoint_response",
				JsonParser.parseString("{\"access_token\":\"at\"}").getAsJsonObject());
			cond.execute(env);
		});
	}

}
