package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateExpiresIn_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodInteger;
	private JsonObject badStringNumeric;
	private JsonObject badNonPrimitive;
	private JsonObject badStringAlpha;

	private ValidateExpiresIn cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ValidateExpiresIn();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodInteger = JsonParser.parseString("{\"expires_in\":3600}").getAsJsonObject();
		badStringNumeric = JsonParser.parseString("{\"expires_in\":\"3600\"}").getAsJsonObject();
		badNonPrimitive = JsonParser.parseString("{\"expires_in\":[1,2,3]}").getAsJsonObject();
		badStringAlpha = JsonParser.parseString("{\"expires_in\":\"fish\"}").getAsJsonObject();

	}

	/**
	 * Test method for {@link ValidateExpiresIn#evaluate(Environment)}.
	 */
	@Test
	public void ValidateExpiresIn_GoodInteger() {

		env.putObject("expires_in", goodInteger);

		cond.execute(env);
	}


	/**
	 * Test method for {@link ValidateExpiresIn#evaluate(Environment)}.
	 */
	@Test
	public void ValidateExpiresIn_BadStringNumeric() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("expires_in", badStringNumeric);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link ValidateExpiresIn#evaluate(Environment)}.
	 */
	@Test
	public void ValidateExpiresIn_BadNonPrimitive() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("expires_in", badNonPrimitive);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link ValidateExpiresIn#evaluate(Environment)}.
	 */
	@Test
	public void ValidateExpiresIn_BadStringAlpha() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("expires_in", badStringAlpha);

			cond.execute(env);
		});
	}

	@Test
	public void ValidateExpiresIn_Zero() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("expires_in", JsonParser.parseString("{\"expires_in\":0}").getAsJsonObject());
			cond.execute(env);
		});
	}

	@Test
	public void ValidateExpiresIn_Negative() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("expires_in", JsonParser.parseString("{\"expires_in\":-1}").getAsJsonObject());
			cond.execute(env);
		});
	}

	@Test
	public void ValidateExpiresIn_NonInteger() {
		// https://tools.ietf.org/html/rfc6749#appendix-A.14 does not actually allow non-integer values,
		// so this unit test is a record that our code is currently not as strict as the spec
		env.putObject("expires_in", JsonParser.parseString("{\"expires_in\":3600.5}").getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void ValidateExpiresIn_WithDecimalPoint() {
		// https://tools.ietf.org/html/rfc6749#appendix-A.14 does not actually allow non-integer values,
		// so this unit test is a record that our code is currently not as strict as the spec
		env.putObject("expires_in", JsonParser.parseString("{\"expires_in\":3600.0}").getAsJsonObject());
		cond.execute(env);
	}

}
