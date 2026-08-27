package net.openid.conformance.condition.as;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class CdrAddCdrArrangementIdToTokenEndpointResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrAddCdrArrangementIdToTokenEndpointResponse cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrAddCdrArrangementIdToTokenEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_generatesAndReusesArrangementId() {
		env.putObject("token_endpoint_response", JsonParser.parseString("{\"access_token\":\"at\"}").getAsJsonObject());

		cond.execute(env);

		String first = OIDFJSON.getString(env.getElementFromObject("token_endpoint_response", "cdr_arrangement_id"));
		assertNotNull(first);
		assertEquals(first, env.getString("cdr_arrangement_id"));

		// a later token response reuses the same arrangement id
		env.putObject("token_endpoint_response", JsonParser.parseString("{\"access_token\":\"at2\"}").getAsJsonObject());
		cond.execute(env);
		assertEquals(first, OIDFJSON.getString(env.getElementFromObject("token_endpoint_response", "cdr_arrangement_id")));
	}

}
