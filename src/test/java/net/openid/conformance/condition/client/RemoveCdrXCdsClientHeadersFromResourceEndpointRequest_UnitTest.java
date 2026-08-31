package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class RemoveCdrXCdsClientHeadersFromResourceEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private RemoveCdrXCdsClientHeadersFromResourceEndpointRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new RemoveCdrXCdsClientHeadersFromResourceEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_removesHeader() {

		env.putObject("resource_endpoint_request_headers",
			JsonParser.parseString("{\"x-cds-client-headers\":\"eyJmb28iOiJiYXIifQ==\",\"x-v\":\"3\"}").getAsJsonObject());

		cond.execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertFalse(req.has("x-cds-client-headers"));
		assertTrue(req.has("x-v"));
	}

	@Test
	public void testEvaluate_headerAbsent() {

		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertFalse(env.getObject("resource_endpoint_request_headers").has("x-cds-client-headers"));
	}

}
