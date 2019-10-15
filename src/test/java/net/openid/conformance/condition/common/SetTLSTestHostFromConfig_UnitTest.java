package net.openid.conformance.condition.common;

import static org.assertj.core.api.Assertions.assertThat;

import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class SetTLSTestHostFromConfig_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetTLSTestHostFromConfig cond;

	private JsonObject tlsConfig = new JsonParser().parse("{"
		+ "\"testHost\":\"example.com\","
		+ "\"testPort\":443"
		+ "}").getAsJsonObject();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new SetTLSTestHostFromConfig();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {

		JsonObject config = new JsonObject();
		config.add("tls", tlsConfig);
		env.putObject("config", config);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("config", "tls.testHost");
		verify(env, atLeastOnce()).getInteger("config", "tls.testPort");

		assertThat(env.getString("tls", "testHost")).isEqualTo(OIDFJSON.getString(tlsConfig.get("testHost")));
		assertThat(env.getInteger("tls", "testPort")).isEqualTo(OIDFJSON.getNumber(tlsConfig.get("testPort")).intValue());
	}

}
