package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ChangeIdTokenToAlgNone_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ChangeIdTokenToAlgNone cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ChangeIdTokenToAlgNone();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_good() {
		JsonObject idTokenObj = new JsonObject();
		idTokenObj.addProperty("value", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InIxTGtiQm8zOTI1UmIyWkZGckt5VTNNVmV4OVQyODE3S3gwdmJpNmlfS2MifQ.eyJzdWIiOiJnb29nbGUuMTAyNDY1OTcyMjQyODEzMTIzNDM2Iiwibm9uY2UiOiJ2YjNtNVRpZ0FpdDNVelhCIiwiYXRfaGFzaCI6IjFEVDlxVk03N1hfbk9oWGJDS01kR1EiLCJhdWQiOiJocVJhMDUxWXRLTDE2b1RaaGs1LUMiLCJleHAiOjE1OTIzMDMxMzMsImlhdCI6MTU5MjI5OTUzMywiaXNzIjoiaHR0cHM6Ly9vcC5wYW52YS5jeiJ9.Nxdnbl5Yfo_StlkwTmUC0D2ApSNneGY9i6DUcsxo1mhfw8rSZC1NiWvsGvHHoWGFUOr0542_8XMenjW1xY3JZSEaj1Hp_gBWRLGoksVWVB8_-6L6Q1nlDZnz_Bq6IxsinEGRcP97iVE8-57bqPNgGNWlJSGtPFSH6DbXzHKYfKqoD533f2Y_pclSAK4HUSecnbutqo_M1TFZjWVIO8_cmJG1K2jLoPJ-hVsz_EbAk5huxwUTb-N9yDPUSijlcDyOxwc2LAUnbeb98dxqvcVOS_xie51FxBVKx66XVottpyXu28QZsx7oZ852TqHrEurmFstJUOTeic8CbtG2IxULQQ");

		env.putObject("id_token", idTokenObj);

		cond.execute(env);

		assertThat(env.getString("id_token", "value")).isEqualTo("eyJhbGciOiAibm9uZSJ9.eyJzdWIiOiJnb29nbGUuMTAyNDY1OTcyMjQyODEzMTIzNDM2Iiwibm9uY2UiOiJ2YjNtNVRpZ0FpdDNVelhCIiwiYXRfaGFzaCI6IjFEVDlxVk03N1hfbk9oWGJDS01kR1EiLCJhdWQiOiJocVJhMDUxWXRLTDE2b1RaaGs1LUMiLCJleHAiOjE1OTIzMDMxMzMsImlhdCI6MTU5MjI5OTUzMywiaXNzIjoiaHR0cHM6Ly9vcC5wYW52YS5jeiJ9.");

	}

}
