package net.openid.conformance.condition.as;

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
public class CheckRequestObjectClaimsParameterMemberValues_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckRequestObjectClaimsParameterMemberValues cond;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckRequestObjectClaimsParameterMemberValues();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CheckRequestObjectClaimsParameterMemberValues#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {
		// All claims have valid values.
		JsonObject authRequestClaims = JsonParser.parseString("{" +
			"    \"claims\": {" +
			"        \"claims\": {" +
			"            \"userinfo\": {" +
			"                \"iss\": null," +
			"                \"sub\": {" +
			"                    \"essential\": true" +
			"                }," +
			"                \"aud\": {" +
			"                    \"value\": \"12345678\"" +
			"                }," +
			"                \"exp\": {" +
			"                    \"values\": [\"12345678\", \"87654321\"]" +
			"                }" +
			"            }," +
			"            \"id_token\": {" +
			"                \"iss\": null," +
			"                \"sub\": {" +
			"                    \"essential\": false" +
			"                }," +
			"                \"aud\": {" +
			"                    \"value\": \"12345678\"" +
			"                }," +
			"                \"exp\": {" +
			"                    \"essential\": true," +
			"                    \"values\": [\"12345678\", \"87654321\"]" +
			"                }," +
			"                \"verified_claims\": {" +
			"                    \"verification\": {" +
			"                        \"trust_framework\": {" +
			"                            \"value\": \"trusted\"" +
			"                        }" +
			"                    }," +
			"                    \"claims\": {" +
			"                        \"over16\": {" +
			"                            \"essential\": true" +
			"                        }," +
			"                        \"over18\": {" +
			"                            \"essential\": true" +
			"                        }" +
			"                    }" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"}")
		.getAsJsonObject();

		env.putObject("authorization_request_object", authRequestClaims);

		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckRequestObjectClaimsParameterMemberValues#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidClaimValue() {
		assertThrows(ConditionError.class, () -> {
			// The 'userinfo->iss' claim value is neither null or an object.
			JsonObject authRequestClaims = JsonParser.parseString("{" +
				"    \"claims\": {" +
				"        \"claims\": {" +
				"            \"userinfo\": {" +
				"                \"iss\": \"12345678\"," +
				"                \"sub\": {" +
				"                    \"essential\": true" +
				"                }," +
				"                \"aud\": {" +
				"                    \"value\": \"12345678\"" +
				"                }," +
				"                \"exp\": {" +
				"                    \"values\": [\"12345678\", \"87654321\"]" +
				"                }" +
				"            }," +
				"            \"id_token\": {" +
				"                \"iss\": null," +
				"                \"sub\": {" +
				"                    \"essential\": false" +
				"                }," +
				"                \"aud\": {" +
				"                    \"value\": \"12345678\"" +
				"                }," +
				"                \"exp\": {" +
				"                    \"essential\": true," +
				"                    \"values\": [\"12345678\", \"87654321\"]" +
				"                }," +
				"                \"verified_claims\": {" +
				"                    \"verification\": {" +
				"                        \"trust_framework\": {" +
				"                            \"value\": \"trusted\"" +
				"                        }" +
				"                    }," +
				"                    \"claims\": {" +
				"                        \"over16\": {" +
				"                            \"essential\": true" +
				"                        }," +
				"                        \"over18\": {" +
				"                            \"essential\": true" +
				"                        }" +
				"                    }" +
				"                }" +
				"            }" +
				"        }" +
				"    }" +
				"}")
			.getAsJsonObject();

			env.putObject("authorization_request_object", authRequestClaims);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link CheckRequestObjectClaimsParameterMemberValues#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidClaimValue1() {
		assertThrows(ConditionError.class, () -> {
			// The 'id_token->sub' claim contains an invalid member
			JsonObject authRequestClaims = JsonParser.parseString("{" +
				"    \"claims\": {" +
				"        \"claims\": {" +
				"            \"userinfo\": {" +
				"                \"iss\": null," +
				"                \"sub\": {" +
				"                    \"essential\": true" +
				"                }," +
				"                \"aud\": {" +
				"                    \"value\": \"12345678\"" +
				"                }," +
				"                \"exp\": {" +
				"                    \"values\": [\"12345678\", \"87654321\"]" +
				"                }" +
				"            }," +
				"            \"id_token\": {" +
				"                \"iss\": null," +
				"                \"sub\": {" +
				"                    \"invalid\": \"1224\"," +
				"                    \"essential\": false" +
				"                }," +
				"                \"aud\": {" +
				"                    \"value\": \"12345678\"" +
				"                }," +
				"                \"exp\": {" +
				"                    \"essential\": true," +
				"                    \"values\": [\"12345678\", \"87654321\"]" +
				"                }," +
				"                \"verified_claims\": {" +
				"                    \"verification\": {" +
				"                        \"trust_framework\": {" +
				"                            \"value\": \"trusted\"" +
				"                        }" +
				"                    }," +
				"                    \"claims\": {" +
				"                        \"over16\": {" +
				"                            \"essential\": true" +
				"                        }," +
				"                        \"over18\": {" +
				"                            \"essential\": true" +
				"                        }" +
				"                    }" +
				"                }" +
				"            }" +
				"        }" +
				"    }" +
				"}")
			.getAsJsonObject();

			env.putObject("authorization_request_object", authRequestClaims);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link CheckRequestObjectClaimsParameterMemberValues#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidClaimValue2() {
		assertThrows(ConditionError.class, () -> {
			// The 'id_token->verified_claims->verification->trust_framework claim value is neither null or an object.
			JsonObject authRequestClaims = JsonParser.parseString("{" +
				"    \"claims\": {" +
				"        \"claims\": {" +
				"            \"userinfo\": {" +
				"                \"iss\": null," +
				"                \"sub\": {" +
				"                    \"essential\": true" +
				"                }," +
				"                \"aud\": {" +
				"                    \"value\": \"12345678\"" +
				"                }," +
				"                \"exp\": {" +
				"                    \"values\": [\"12345678\", \"87654321\"]" +
				"                }" +
				"            }," +
				"            \"id_token\": {" +
				"                \"iss\": null," +
				"                \"sub\": {" +
				"                    \"essential\": false" +
				"                }," +
				"                \"aud\": {" +
				"                    \"value\": \"12345678\"" +
				"                }," +
				"                \"exp\": {" +
				"                    \"essential\": true," +
				"                    \"values\": [\"12345678\", \"87654321\"]" +
				"                }," +
				"                \"verified_claims\": {" +
				"                    \"verification\": {" +
				"                        \"trust_framework\": \"12345678\"" +
				"                    }," +
				"                    \"claims\": {" +
				"                        \"over16\": {" +
				"                            \"essential\": true" +
				"                        }," +
				"                        \"over18\": {" +
				"                            \"essential\": true" +
				"                        }" +
				"                    }" +
				"                }" +
				"            }" +
				"        }" +
				"    }" +
				"}")
			.getAsJsonObject();

			env.putObject("authorization_request_object", authRequestClaims);

			cond.execute(env);
		});
	}
	/**
	 * Test method for {@link CheckRequestObjectClaimsParameterMemberValues#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidClaimValueInEach() {
		assertThrows(ConditionError.class, () -> {
			// The 'userinfo->iss' claim value is neither null or an object.
			// the 'id_token->sub' claim contains an invalid member and
			// the 'id_token->verified_claims->claims-<over16' claim contains an invalid member.
			JsonObject authRequestClaims = JsonParser.parseString("{" +
				"    \"claims\": {" +
				"        \"claims\": {" +
				"            \"userinfo\": {" +
				"                \"iss\": \"12345678\"," +
				"                \"sub\": {" +
				"                    \"essential\": true" +
				"                }," +
				"                \"aud\": {" +
				"                    \"value\": \"12345678\"" +
				"                }," +
				"                \"exp\": {" +
				"                    \"values\": [\"12345678\", \"87654321\"]" +
				"                }" +
				"            }," +
				"            \"id_token\": {" +
				"                \"iss\": null," +
				"                \"sub\": {" +
				"                    \"invalid\": \"1224\"," +
				"                    \"essential\": false" +
				"                }," +
				"                \"aud\": {" +
				"                    \"value\": \"12345678\"" +
				"                }," +
				"                \"exp\": {" +
				"                    \"essential\": true," +
				"                    \"values\": [\"12345678\", \"87654321\"]" +
				"                }," +
				"                \"verified_claims\": {" +
				"                    \"verification\": {" +
				"                        \"trust_framework\": {" +
				"                            \"value\": \"trusted\"" +
				"                        }" +
				"                    }," +
				"                    \"claims\": {" +
				"                        \"over16\": {" +
				"                            \"invalid\": true" +
				"                        }," +
				"                        \"over18\": {" +
				"                            \"essential\": true" +
				"                        }" +
				"                    }" +
				"                }" +
				"            }" +
				"        }" +
				"    }" +
				"}")
			.getAsJsonObject();

			env.putObject("authorization_request_object", authRequestClaims);

			cond.execute(env);
		});
	}
}
