package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CreateRevokedStatusListReference_UnitTest {

	private static final String BASE_URL = "https://localhost.emobix.co.uk:8443/test/a/alias";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateRevokedStatusListReference cond;

	@BeforeEach
	public void setUp() {
		cond = new CreateRevokedStatusListReference();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_buildsTheUriFromTheTestInstanceBaseUrl() {
		env.putString("base_url", BASE_URL);

		cond.execute(env);

		assertThat(OIDFJSON.getString(
			env.getElementFromObject(AbstractCreateStatusListReference.ENV_KEY, "uri")))
			.isEqualTo(BASE_URL + "/statuslists/1");
	}

	@RepeatedTest(20)
	public void testEvaluate_allocatesAnIndexTheServedListMarksRevoked() {
		env.putString("base_url", BASE_URL);

		cond.execute(env);

		int idx = OIDFJSON.getInt(
			env.getElementFromObject(AbstractCreateStatusListReference.ENV_KEY, "idx"));
		assertThat(idx).isBetween(0, EvenOddStatusListContents.STATUS_LIST_ENTRIES - 1);
		assertThat(idx % 2).as("only odd indices are revoked in the served list").isEqualTo(1);

		TokenStatusList statusList = EvenOddStatusListContents.create();
		assertThat(statusList.getStatus(idx)).isEqualTo(TokenStatusList.Status.INVALID);
		// a neighbouring even index is still valid, i.e. the list is not revoked wholesale
		assertThat(statusList.getStatus(idx - 1)).isEqualTo(TokenStatusList.Status.VALID);
	}

	@Test
	public void testEvaluate_failsWithoutABaseUrl() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
