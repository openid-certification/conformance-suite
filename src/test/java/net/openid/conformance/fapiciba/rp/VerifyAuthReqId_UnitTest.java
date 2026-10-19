package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class VerifyAuthReqId_UnitTest {
	private final Environment env = new Environment();
	private final VerifyAuthReqId condition = new VerifyAuthReqId();
	private final TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);

	@BeforeEach
	public void initialize() {
		condition.setProperties("test", eventLog, Condition.ConditionResult.FAILURE);
		env.putString("auth_req_id", "first");
		env.putObjectFromJsonString("token_endpoint_request", """
			{"body_form_params":{"auth_req_id":"first"}}
			""");
	}

	@Test
	public void acceptsMatchingUnredeemedId() {
		assertThatCode(() -> condition.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void rejectsIdAfterSuccessfulRedemption() {
		RedeemAuthReqId redeem = new RedeemAuthReqId();
		redeem.setProperties("test", eventLog, Condition.ConditionResult.FAILURE);
		redeem.execute(env);

		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("already been redeemed");
	}

	@Test
	public void rejectsDifferentId() {
		env.putString("auth_req_id", "second");

		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("Mismatch in auth_req_id");
	}
}
