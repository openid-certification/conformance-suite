package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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

	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractFapiIpAddressHeader();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-customer-ip-address\": \"" + addr + "\""
			+ "}}").getAsJsonObject();
		good6Request = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-customer-ip-address\": \"" + addr6 + "\""
			+ "}}").getAsJsonObject();
		badRequest = JsonParser.parseString("{\"headers\":{"
			+ "\"x-fapi-customer-ip-address\": \"" + badAddr + "\""
			+ "}}").getAsJsonObject();
		missingHeader = JsonParser.parseString("{\"headers\":{}}").getAsJsonObject();
		noHeaders = JsonParser.parseString("{}").getAsJsonObject();

	}

	@Test
	public void test_good() {

		env.putObject("incoming_request", goodRequest);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("incoming_request", "headers.x-fapi-customer-ip-address");
		assertEquals(addr, env.getString("fapi_customer_ip_address"));
	}

	@Test
	public void test_good6() {

		env.putObject("incoming_request", good6Request);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("incoming_request", "headers.x-fapi-customer-ip-address");
		assertEquals(addr6, env.getString("fapi_customer_ip_address"));
	}

	@Test
	public void test_bad() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", badRequest);
			cond.execute(env);
		});
	}

	@Test
	public void test_missing() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", missingHeader);
			cond.execute(env);
		});
	}

	@Test
	public void test_noHeader() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("incoming_request", noHeaders);
			cond.execute(env);
		});
	}

}
