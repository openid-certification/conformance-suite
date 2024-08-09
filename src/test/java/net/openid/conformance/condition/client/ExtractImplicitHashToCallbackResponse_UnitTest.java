package net.openid.conformance.condition.client;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractImplicitHashToCallbackResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractImplicitHashToCallbackResponse cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractImplicitHashToCallbackResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {

		env.putString("implicit_hash", "#foo=1&bar=2");

		cond.execute(env);

		assertThat(env.getObject("callback_params").size() == 2);
		assertThat(env.getString("callback_params", "foo")).isEqualTo("1");
		assertThat(env.getString("callback_params", "bar")).isEqualTo("2");
	}

	@Test
	public void testEvaluate_empty() {

		env.putString("implicit_hash", "");

		cond.execute(env);

		assertThat(env.getObject("callback_params").size() == 0);
	}

	@Test
	public void testEvaluate_onlyHash() {

		env.putString("implicit_hash", "#");

		cond.execute(env);

		assertThat(env.getObject("callback_params").size() == 0);
	}

	@Test
	public void testEvaluate_noValues() {

		env.putString("implicit_hash", "#foo&bar");

		cond.execute(env);

		assertThat(env.getObject("callback_params").size() == 2);
		assertThat(env.getElementFromObject("callback_params", "foo").isJsonNull());
		assertThat(env.getElementFromObject("callback_params", "bar").isJsonNull());
	}

	@Test
	public void testEvaluate_semicolon() {

		env.putString("implicit_hash", "#foo=1;2&bar=3");

		cond.execute(env);

		assertThat(env.getObject("callback_params").size() == 2);
		assertThat(env.getString("callback_params", "foo")).isEqualTo("1;2");
		assertThat(env.getString("callback_params", "bar")).isEqualTo("3");
	}

	@Test
	public void testEvaluate_missingInput() {
		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

}
