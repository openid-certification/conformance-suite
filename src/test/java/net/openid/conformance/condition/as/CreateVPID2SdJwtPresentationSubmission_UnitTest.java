package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CreateVPID2SdJwtPresentationSubmission_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateVPID2SdJwtPresentationSubmission cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateVPID2SdJwtPresentationSubmission();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		String pd = """
			{
			    "id": "4db74328-9e94-49bb-97b7-bbfcb2d11a06",
			    "input_descriptors": [
			      { "id": "fdf340c6-05f9-4125-b0fc-86a08d8ad051" }
			    ]
			}""";
		env.putObjectFromJsonString("authorization_request_object", "claims.presentation_definition", pd);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_throwsWhenPresentationDefinitionMissing() {
		// e.g. a DCQL request carries no presentation_definition
		env.putObjectFromJsonString("authorization_request_object", "claims", "{}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
