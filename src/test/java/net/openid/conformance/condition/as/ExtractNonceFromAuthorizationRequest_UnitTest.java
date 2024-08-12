package net.openid.conformance.condition.as;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExtractNonceFromAuthorizationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractNonceFromAuthorizationRequest cond;

	private String nonce = "123456";

	private JsonObject hasNonce;
	private JsonObject noNonce;
	private JsonObject onlyNonce;
	private JsonObject noParams;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractNonceFromAuthorizationRequest();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		hasNonce = JsonParser.parseString("{\"nonce\": \"" + nonce + "\", \"state\": \"843192\"}").getAsJsonObject();
		noNonce = JsonParser.parseString("{\"state\": \"843192\"}").getAsJsonObject();
		onlyNonce = JsonParser.parseString("{\"nonce\": \"" + nonce + "\"}").getAsJsonObject();
		noParams = JsonParser.parseString("{}").getAsJsonObject();

	}

	@Test
	public void test_good() {

		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, hasNonce);
		cond.execute(env);

		verify(env, atLeastOnce()).getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);
		verify(env, times(1)).putString("nonce", nonce);

		assertEquals(env.getString("nonce"), nonce);
	}

	@Test
	public void test_only() {

		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, onlyNonce);
		cond.execute(env);

		verify(env, atLeastOnce()).getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);
		verify(env, times(1)).putString("nonce", nonce);

		assertEquals(env.getString("nonce"), nonce);

	}

	@Test
	public void test_bad() {
		assertThrows(ConditionError.class, () -> {

			env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, noNonce);
			cond.execute(env);

		});

	}
	@Test
	public void test_missing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, noParams);
			cond.execute(env);

		});

	}
}
