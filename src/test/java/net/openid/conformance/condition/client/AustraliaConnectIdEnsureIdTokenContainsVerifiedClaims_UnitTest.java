package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
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
public class AustraliaConnectIdEnsureIdTokenContainsVerifiedClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdEnsureIdTokenContainsVerifiedClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdEnsureIdTokenContainsVerifiedClaims();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_allClaimsValid() {
		JsonArray supportedVerifiedClaims = JsonParser.parseString(
		"""
		[
		  "over16",
		  "over18",
		  "over21",
		  "over25",
		  "over65",
		  "beneficiary_account_au",
		  "beneficiary_account_au_payid",
		  "beneficiary_account_international"
		]
		""").getAsJsonArray();

		env.putObject("server", new JsonObject());
		env.getObject("server").add("claims_in_verified_claims_supported", supportedVerifiedClaims);

		JsonObject requestObject = JsonParser.parseString(
		"""
		{
		  "claims": {
		    "id_token": {
		      "verified_claims": {
		        "claims": {
		          "over16":  {
		            essential: true
		          },
		          "over18": {
		            essential: true
		          },
		          "over21": {
		            essential: true
		          },
		          "over25": {
		            essential: true
		          },
		          "over65": {
		            essential: true
		          },
		          "beneficiary_account_au": {
		            essential: false
		          },
		          "beneficiary_account_au_payid": {
		            essential: false
		          },
		          "beneficiary_account_international": {
		            essential: false
		          }
		        }
		      }
		    }
		  }
		}
		""").getAsJsonObject();

		env.putObject("authorization_endpoint_request", requestObject);

		JsonObject idToken = JsonParser.parseString(
		"""
		{
		  "claims": {
		    "verified_claims": {
		      "claims": {
		        "over16": true,
		        "over18": true,
		        "over21": true,
		        "over25": true,
		        "over65": true,
		        "beneficiary_account_au": {
		          "beneficiary_name": "John Smith",
		          "account_bsb": "100200",
		          "account_number": "12345678"
		        },
		        "beneficiary_account_au_payid": {
		          "beneficiary_name": "John Smith",
		          "payid": "0400000321",
		          "payid_type": "/TELI"
		        },
		        "beneficiary_account_international": {
		          "beneficiary_name": "John Smith",
		          "bic_swift_code": "XXXXXNNNNX",
		          "account_number_international": "10020012345678",
		          "beneficiary_residential_address": "255 George St, Sydney, NSW 2000, Australia"
		        }
		      }
		    }
		  }
		}
		""").getAsJsonObject();

		env.putObject("id_token", idToken);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingRequestedUnsupportedClaim() {
		// The 'over16' claim is not supported by the server and this missing from the id_token even though requested.
		JsonArray supportedVerifiedClaims = JsonParser.parseString(
		"""
		[
		  "over18",
		  "over21",
		  "over25",
		  "over65",
		  "beneficiary_account_au",
		  "beneficiary_account_au_payid",
		  "beneficiary_account_international"
		]
		""").getAsJsonArray();

		env.putObject("server", new JsonObject());
		env.getObject("server").add("claims_in_verified_claims_supported", supportedVerifiedClaims);

		JsonObject requestObject = JsonParser.parseString(
		"""
		{
		  "claims": {
		    "id_token": {
		      "verified_claims": {
		        "claims": {
		          "over16":  {
		            essential: true
		          },
		          "over18": {
		            essential: true
		          },
		          "over21": {
		            essential: true
		          },
		          "over25": {
		            essential: true
		          },
		          "over65": {
		            essential: true
		          },
		          "beneficiary_account_au": {
		            essential: true
		          },
		          "beneficiary_account_au_payid": {
		            essential: true
		          },
		          "beneficiary_account_international": {
		            essential: true
		          }
		        }
		      }
		    }
		  }
		}
		""").getAsJsonObject();

		env.putObject("authorization_endpoint_request", requestObject);

		JsonObject idToken = JsonParser.parseString(
		"""
		{
		  "claims": {
		    "verified_claims": {
		      "claims": {
		        "over18": true,
		        "over21": true,
		        "over25": true,
		        "over65": true,
		        "beneficiary_account_au": {
		          "beneficiary_name": "John Smith",
		          "account_bsb": "100200",
		          "account_number": "12345678"
		        },
		        "beneficiary_account_au_payid": {
		          "beneficiary_name": "John Smith",
		          "payid": "0400000321",
		          "payid_type": "/TELI"
		        },
		        "beneficiary_account_international": {
		          "beneficiary_name": "John Smith",
		          "bic_swift_code": "XXXXXNNNNX",
		          "account_number_international": "10020012345678",
		          "beneficiary_residential_address": "255 George St, Sydney, NSW 2000, Australia"
		        }
		      }
		    }
		  }
		}
		""").getAsJsonObject();

		env.putObject("id_token", idToken);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingRequestedSupportedClaim() {
		// The 'over16' claim is supported by the server and requested, but is unexpectedly missing from the id_token.
		assertThrows(ConditionError.class, () -> {
		  JsonArray supportedVerifiedClaims = JsonParser.parseString(
		  """
		  [
		    "over16",
		    "over18",
		    "over21",
		    "over25",
		    "over65",
		    "beneficiary_account_au",
		    "beneficiary_account_au_payid",
		    "beneficiary_account_international"
		  ]
		  """).getAsJsonArray();

		  env.putObject("server", new JsonObject());
		  env.getObject("server").add("claims_in_verified_claims_supported", supportedVerifiedClaims);

		  JsonObject requestObject = JsonParser.parseString(
		  """
		  {
		    "claims": {
		      "id_token": {
		        "verified_claims": {
		          "claims": {
		            "over16":  {
		              essential: true
		            },
		            "over18": {
		              essential: true
		            },
		            "over21": {
		              essential: true
		            },
		            "over25": {
		              essential: true
		            },
		            "over65": {
		              essential: true
		            },
		            "beneficiary_account_au": {
		              essential: true
		            },
		            "beneficiary_account_au_payid": {
		              essential: true
		            },
		            "beneficiary_account_international": {
		              essential: true
		            }
		          }
		        }
		      }
		    }
		  }
		  """).getAsJsonObject();

		  env.putObject("authorization_endpoint_request", requestObject);

		  JsonObject idToken = JsonParser.parseString(
		  """
		  {
		    "claims": {
		      "verified_claims": {
		        "claims": {
		          "over18": true,
		          "over21": true,
		          "over25": true,
		          "over65": true,
		          "beneficiary_account_au": {
		            "beneficiary_name": "John Smith",
		            "account_bsb": "100200",
		            "account_number": "12345678"
		          },
		          "beneficiary_account_au_payid": {
		            "beneficiary_name": "John Smith",
		            "payid": "0400000321",
		            "payid_type": "/TELI"
		          },
		          "beneficiary_account_international": {
		            "beneficiary_name": "John Smith",
		            "bic_swift_code": "XXXXXNNNNX",
		            "account_number_international": "10020012345678",
		            "beneficiary_residential_address": "255 George St, Sydney, NSW 2000, Australia"
		          }
		        }
		      }
		    }
		  }
		  """).getAsJsonObject();

		  env.putObject("id_token", idToken);

		  cond.execute(env);

		});
	}

	@Test
	public void testEvaluate_unexpectedClaim() {
		// The id_token contains an unrequested claim.
		assertThrows(ConditionError.class, () -> {
		  JsonArray supportedVerifiedClaims = JsonParser.parseString(
		  """
		  [
		    "over16",
		    "over18",
		    "over21",
		    "over25",
		    "over65",
		    "beneficiary_account_au",
		    "beneficiary_account_au_payid",
		    "beneficiary_account_international"
		  ]
		  """).getAsJsonArray();

		  env.putObject("server", new JsonObject());
		  env.getObject("server").add("claims_in_verified_claims_supported", supportedVerifiedClaims);

		  JsonObject requestObject = JsonParser.parseString(
		  """
		  {
		    "claims": {
		      "id_token": {
		        "verified_claims": {
		          "claims": {
		            "over16":  {
		              essential: true
		            },
		            "over18": {
		              essential: true
		            },
		            "over21": {
		              essential: true
		            },
		            "over25": {
		              essential: true
		            },
		            "over65": {
		              essential: true
		            },
		            "beneficiary_account_au": {
		              essential: true
		            },
		            "beneficiary_account_au_payid": {
		              essential: true
		            },
		            "beneficiary_account_international": {
		              essential: true
		            }
		          }
		        }
		      }
		    }
		  }
		  """).getAsJsonObject();

		  env.putObject("authorization_endpoint_request", requestObject);

		  JsonObject idToken = JsonParser.parseString(
		  """
		  {
		    "claims": {
		      "verified_claims": {
		        "claims": {
		          "unexpected": true,
		          "over16": true,
		          "over18": true,
		          "over21": true,
		          "over25": true,
		          "over65": true,
		          "beneficiary_account_au": {
		            "beneficiary_name": "John Smith",
		            "account_bsb": "100200",
		            "account_number": "12345678"
		          },
		          "beneficiary_account_au_payid": {
		            "beneficiary_name": "John Smith",
		            "payid": "0400000321",
		            "payid_type": "/TELI"
		          },
		          "beneficiary_account_international": {
		            "beneficiary_name": "John Smith",
		            "bic_swift_code": "XXXXXNNNNX",
		            "account_number_international": "10020012345678",
		            "beneficiary_residential_address": "255 George St, Sydney, NSW 2000, Australia"
		          }
		        }
		      }
		    }
		  }
		  """).getAsJsonObject();

		  env.putObject("id_token", idToken);

		  cond.execute(env);

		});
	}
}
