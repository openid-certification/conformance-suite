package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenBankingBrazilCibaBackchannelRequest_UnitTest {

	private Environment env;
	private EnsureBackchannelRequestObjectDoesNotContainUserCode condition;

	@BeforeEach
	public void setup() {
		env = mock(Environment.class);
		condition = new EnsureBackchannelRequestObjectDoesNotContainUserCode();
		condition.setProperties("testId", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
	}

	@Test
	public void succeedsWhenUserCodeIsMissing() {
		condition.evaluate(env);
	}

	@Test
	public void failsWhenUserCodeIsPresent() {
		when(env.getElementFromObject("backchannel_request_object", "claims.user_code"))
			.thenReturn(new JsonPrimitive("123456"));

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("not permitted for Open Finance Brasil CIBA");
	}

}
