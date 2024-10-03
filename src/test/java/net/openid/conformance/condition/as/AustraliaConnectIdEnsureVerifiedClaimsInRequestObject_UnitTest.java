package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AustraliaConnectIdEnsureVerifiedClaimsInRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdEnsureVerifiedClaimsInRequestObject cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdEnsureVerifiedClaimsInRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_allClaims() {
		JsonObject requestObject = JsonParser.parseString(
		"""
		{
		  "claims": {
		    "claims": {
		      "id_token": {
		        "verified_claims": {
		          "claims": {
		            "over16": {
		              "essential": true
		            },
		            "over18": {
		              "essential": true
		            },
		            "over21": {
		              "essential": true
		            },
		            "over25": {
		              "essential": true
		            },
		            "over65": {
		              "essential": true
		            },
		            "beneficiary_account_au": {
		              "essential": true
		            },
		            "beneficiary_account_au_payid": {
		              "essential": true
		            },
		            "beneficiary_account_international": {
		              "essential": true
		            }
		          }
		        }
		      }
		    }
		  }
		}
		""").getAsJsonObject();

		env.putObject("authorization_request_object", requestObject);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_singleClaim() {
		JsonObject requestObject = JsonParser.parseString(
		"""
		{
		  "claims": {
		    "claims": {
		      "id_token": {
		        "verified_claims": {
		          "claims": {
		            "over16": {
		              "essential": true
		            }
		          }
		        }
		      }
		    }
		  }
		}
		""").getAsJsonObject();

		env.putObject("authorization_request_object", requestObject);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noClaim() {
		JsonObject requestObject = JsonParser.parseString(
		"""
		{
		  "claims": {
		    "claims": {
		      "id_token": {
		        "verified_claims": {
		          "claims": {
		          }
		        }
		      }
		    }
		  }
		}
		""").getAsJsonObject();

		env.putObject("authorization_request_object", requestObject);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidClaim() {
		assertThrows(ConditionError.class, () -> {
			JsonObject requestObject = JsonParser.parseString(
			"""
			{
			  "claims": {
			    "claims": {
			      "id_token": {
			        "verified_claims": {
			          "claims": {
			            "invalid": {
			              "essential": true
			            }
			          }
			        }
			      }
			    }
			  }
			}
			""").getAsJsonObject();

			env.putObject("authorization_request_object", requestObject);
			cond.execute(env);
		});
	}
}
