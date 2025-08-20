package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidateVerifiedClaimsRequestAgainstSchema_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVerifiedClaimsRequestAgainstSchema cond;

	@BeforeEach
	void setUp() {
		cond = new 	ValidateVerifiedClaimsRequestAgainstSchema();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}


	protected void runTest(String requestString) throws Exception {
		env.putObject("authorization_endpoint_request", JsonParser.parseString(requestString).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError_userinfo() throws Exception {
		String request = "{" +
			"    \"claims\": {" +
			"        \"userinfo\": {" +
			"            \"verified_claims\": {" +
			"                \"claims\": {" +
			"                    \"birthdate\": null," +
			"                    \"given_name\": null," +
			"                    \"family_name\": null" +
			"                }," +
			"                \"verification\": {" +
			"                    \"trust_framework\": {" +
			"                        \"value\": \"de_aml\"" +
			"                    }," +
			"                    \"evidence\": [" +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"document\"" +
			"                            }" +
			"                        }," +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"electronic_record\"" +
			"                            }" +
			"                        }" +
			"                    ]" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"}";
		runTest(request);
	}

	@Test
	public void testEvaluate_noError_idtoken() throws Exception {
		String request = "{" +
			"    \"claims\": {" +
			"        \"id_token\": {" +
			"            \"verified_claims\": {" +
			"                \"claims\": {" +
			"                    \"birthdate\": null," +
			"                    \"given_name\": null," +
			"                    \"family_name\": null" +
			"                }," +
			"                \"verification\": {" +
			"                    \"trust_framework\": {" +
			"                        \"value\": \"de_aml\"" +
			"                    }," +
			"                    \"evidence\": [" +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"document\"" +
			"                            }" +
			"                        }," +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"electronic_record\"" +
			"                            }" +
			"                        }" +
			"                    ]" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"}";
		runTest(request);
	}

	@Test
	public void testEvaluate_fail_invalid_evidence_type() throws Exception {
		String request = "{" +
			"    \"claims\": {" +
			"        \"userinfo\": {" +
			"            \"verified_claims\": {" +
			"                \"claims\": {" +
			"                    \"birthdate\": null," +
			"                    \"given_name\": null," +
			"                    \"family_name\": null" +
			"                }," +
			"                \"verification\": {" +
			"                    \"trust_framework\": {" +
			"                        \"value\": \"de_aml\"" +
			"                    }," +
			"                    \"evidence\": [" +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"documentxxx\"" +
			"                            }" +
			"                        }," +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"electronic_record\"" +
			"                            }" +
			"                        }" +
			"                    ]" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"}";

		assertThrows(ConditionError.class, () -> {
			runTest(request);
		});

	}


	@Test
	public void testEvaluate_fail_no_trustframework() throws Exception {
		String request = "{" +
			"    \"claims\": {" +
			"        \"userinfo\": {" +
			"            \"verified_claims\": {" +
			"                \"claims\": {" +
			"                    \"birthdate\": null," +
			"                    \"given_name\": null," +
			"                    \"family_name\": null" +
			"                }," +
			"                \"verification\": {" +
			"                    \"trust_frameworkXXX\": {" +
			"                        \"value\": \"de_aml\"" +
			"                    }," +
			"                    \"evidence\": [" +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"document\"" +
			"                            }" +
			"                        }," +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"electronic_record\"" +
			"                            }" +
			"                        }" +
			"                    ]" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"}";

		assertThrows(ConditionError.class, () -> {
			runTest(request);
		});

	}

	@Test
	public void testEvaluate_fail_idtoken_no_claims() throws Exception {
		String request = "{" +
			"    \"claims\": {" +
			"        \"id_token\": {" +
			"            \"verified_claims\": {" +
			"                \"claimsXXX\": {" +
			"                    \"birthdate\": null," +
			"                    \"given_name\": null," +
			"                    \"family_name\": null" +
			"                }," +
			"                \"verification\": {" +
			"                    \"trust_framework\": {" +
			"                        \"value\": \"de_aml\"" +
			"                    }," +
			"                    \"evidence\": [" +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"document\"" +
			"                            }" +
			"                        }," +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"electronic_record\"" +
			"                            }" +
			"                        }" +
			"                    ]" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"}";

		assertThrows(ConditionError.class, () -> {
			runTest(request);
		});

	}

	@Test
	public void testEvaluate_fail_null_evidence() throws Exception {
		String request = "{" +
			"    \"claims\": {" +
			"        \"id_token\": {" +
			"            \"verified_claims\": {" +
			"                \"claims\": {" +
			"                    \"birthdate\": null," +
			"                    \"given_name\": null," +
			"                    \"family_name\": null" +
			"                }," +
			"                \"verification\": {" +
			"                    \"trust_framework\": {" +
			"                        \"value\": \"de_aml\"" +
			"                    }," +
			"                    \"evidence\": [" +
			"                        {" +
			"                            \"type\": null" +
			"                        }," +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"document\"" +
			"                            }" +
			"                        }," +
			"                        {" +
			"                            \"type\": {" +
			"                                \"value\": \"electronic_record\"" +
			"                            }" +
			"                        }" +
			"                    ]" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"}";

		assertThrows(ConditionError.class, () -> {
			runTest(request);
		});

	}
}
