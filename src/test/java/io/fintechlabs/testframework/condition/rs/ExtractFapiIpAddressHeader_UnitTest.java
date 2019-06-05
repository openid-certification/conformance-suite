package io.fintechlabs.testframework.condition.rs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertEquals;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.rs.ExtractFapiIpAddressHeader;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ExtractFapiIpAddressHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractFapiIpAddressHeader cond;

	private String addr = "198.51.100.119"; // example from FAPI spec
	private String addr6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
	private String badAddr = "300.200.333.10231";

	private JsonObject goodRequest;
	private JsonObject good6Request;
	private JsonObject badRequest;
	private JsonObject missingHeader;
	private JsonObject noHeaders;

	@Before
	public void setUp() throws Exception {

		cond = new ExtractFapiIpAddressHeader();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-customer-ip-address\": \"" + addr + "\""
			+ "}}").getAsJsonObject();
		good6Request = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-customer-ip-address\": \"" + addr6 + "\""
			+ "}}").getAsJsonObject();
		badRequest = new JsonParser().parse("{\"headers\":{"
			+ "\"x-fapi-customer-ip-address\": \"" + badAddr + "\""
			+ "}}").getAsJsonObject();
		missingHeader = new JsonParser().parse("{\"headers\":{}}").getAsJsonObject();
		noHeaders = new JsonParser().parse("{}").getAsJsonObject();

	}

	@Test
	public void test_good() {

		env.putObject("incoming_request", goodRequest);
		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("incoming_request", "headers.x-fapi-customer-ip-address");
		assertEquals(addr, env.getString("fapi_customer_ip_address"));
	}

	@Test
	public void test_good6() {

		env.putObject("incoming_request", good6Request);
		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("incoming_request", "headers.x-fapi-customer-ip-address");
		assertEquals(addr6, env.getString("fapi_customer_ip_address"));
	}

	@Test(expected = ConditionError.class)
	public void test_bad() {
		env.putObject("incoming_request", badRequest);
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void test_missing() {
		env.putObject("incoming_request", missingHeader);
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void test_noHeader() {
		env.putObject("incoming_request", noHeaders);
		cond.evaluate(env);
	}

}
