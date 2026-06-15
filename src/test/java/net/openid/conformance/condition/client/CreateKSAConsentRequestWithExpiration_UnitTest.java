package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
public class CreateKSAConsentRequestWithExpiration_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateKSAConsentRequestWithExpiration cond;
	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new CreateKSAConsentRequestWithExpiration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
		env.putString("client_id", "client-1234");
	}

	@Test
	public void testExpirationIsInTheFuture() {
		env = cond.evaluate(env);

		JsonObject data = env.getObject("account_requests_endpoint_request")
			.getAsJsonObject("message").getAsJsonObject("Data");
		String expiration = OIDFJSON.getString(data.get("ExpirationDateTime"));
		assertThat(OffsetDateTime.parse(expiration).isAfter(OffsetDateTime.now()), is(true));
	}
}
