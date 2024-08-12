package net.openid.conformance.ekyc.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
public class ValidateVerifiedClaimsInUserinfoResponseAgainstRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVerifiedClaimsInUserinfoResponseAgainstRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVerifiedClaimsInUserinfoResponseAgainstRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	protected void runTest(String requestFilename, String responseFilename) throws IOException {
		String requestJson = IOUtils.resourceToString(requestFilename, StandardCharsets.UTF_8, getClass().getClassLoader());
		String responseJson = IOUtils.resourceToString(responseFilename, StandardCharsets.UTF_8, getClass().getClassLoader());

		env.putObjectFromJsonString("authorization_endpoint_request", "claims.userinfo.verified_claims", requestJson);
		env.putObjectFromJsonString("verified_claims_response", "userinfo", responseJson);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError() throws Exception {
		String requestFilename = "ValidateVerifiedClaimsInUserinfoResponseAgainstRequest/request.json";
		String responseFilename = "ValidateVerifiedClaimsInUserinfoResponseAgainstRequest/response.json";

		runTest(requestFilename, responseFilename);
	}
}
