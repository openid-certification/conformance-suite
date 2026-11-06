package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AddPoppyHeadersToResourceRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddPoppyHeadersToResourceRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new AddPoppyHeadersToResourceRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void addsHeadersKeepingExisting() {
		JsonObject h = new JsonObject();
		h.addProperty("X-Other", "1");
		env.putObject("resource_endpoint_request_headers", h);
		env.putString("poppy_session_jwt", "jwt");
		cond.execute(env);
		JsonObject out = env.getObject("resource_endpoint_request_headers");
		assertEquals("jwt", env.getString("resource_endpoint_request_headers", "Poppy-Session"));
		assertEquals("0.4", env.getString("resource_endpoint_request_headers", "Poppy-Version"));
		assertEquals("1", env.getString("resource_endpoint_request_headers", "X-Other"));
		assertEquals(3, out.size());
	}

	@Test
	public void createsHeadersWhenAbsent() {
		env.putString("poppy_session_jwt", "jwt");
		cond.execute(env);
		assertEquals("jwt", env.getString("resource_endpoint_request_headers", "Poppy-Session"));
	}
}
