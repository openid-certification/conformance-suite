package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidateVerifiedClaimsRequestAgainstSchema_UnitTest {
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

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

	@Test
	public void testEvaluate_noError_assurance_details_evidence_ref_with_check_id() {
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {
			          "given_name": null
			        },
			        "verification": {
			          "trust_framework": {
			            "value": "de_aml"
			          },
			          "assurance_process": {
			            "assurance_details": [
			              {
			                "evidence_ref": [
			                  {
			                    "check_id": {
			                      "value": "chk-1"
			                    },
			                    "evidence_metadata": {
			                      "evidence_classification": {
			                        "value": "evc1"
			                      }
			                    }
			                  }
			                ]
			              }
			            ]
			          }
			        }
			      }
			    }
			  }
			}
			""";

		assertDoesNotThrow(() -> runTest(request));
	}

	@Test
	public void testEvaluate_fail_assurance_details_evidence_ref_missing_check_id() {
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {
			          "given_name": null
			        },
			        "verification": {
			          "trust_framework": {
			            "value": "de_aml"
			          },
			          "assurance_process": {
			            "assurance_details": [
			              {
			                "evidence_ref": [
			                  {
			                    "evidence_metadata": {
			                      "evidence_classification": {
			                        "value": "evc1"
			                      }
			                    }
			                  }
			                ]
			              }
			            ]
			          }
			        }
			      }
			    }
			  }
			}
			""";

		assertThrows(ConditionError.class, () -> runTest(request));
	}

	@Test
	public void testEvaluate_noError_essential_on_evidence_type() {
		// IDA section 5.3 allows essential on requested elements; evidence/type is no exception.
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {"given_name": null},
			        "verification": {
			          "trust_framework": {"value": "de_aml"},
			          "evidence": [{
			            "type": {"essential": true, "value": "document"},
			            "document_details": {"type": null}
			          }]
			        }
			      }
			    }
			  }
			}
			""";

		assertDoesNotThrow(() -> runTest(request));
	}

	@Test
	public void testEvaluate_fail_values_on_evidence_type() {
		// IDA section 5.4: "The values sub-element shall not be used for the evidence/type
		// field." - a shall-not, so it must fail this (FAILURE) condition rather than only
		// the unknown-property warning.
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {"given_name": null},
			        "verification": {
			          "trust_framework": {"value": "de_aml"},
			          "evidence": [{
			            "type": {"values": ["document", "vouch"]}
			          }]
			        }
			      }
			    }
			  }
			}
			""";

		assertThrows(ConditionError.class, () -> runTest(request));
	}

	@Test
	public void testEvaluate_fail_evidence_type_without_value() {
		// IDA section 5.4: "The RP therefore shall specify this type by including the type field
		// including a suitable value sub-element value." - a shall, so an evidence entry whose
		// type has no value must fail this (FAILURE) condition.
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {"given_name": null},
			        "verification": {
			          "trust_framework": {"value": "de_aml"},
			          "evidence": [{
			            "type": {"essential": true}
			          }]
			        }
			      }
			    }
			  }
			}
			""";

		assertThrows(ConditionError.class, () -> runTest(request));
	}

	@Test
	public void testEvaluate_noError_unknown_property_in_document_details() {
		// Unknown properties are reported (as a warning) by
		// CheckForUnexpectedPropertiesInVerifiedClaimsRequest, not by this condition.
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.REQUEST_UNKNOWN_PROPERTY_IN_DOCUMENT_DETAILS));
	}

	@Test
	public void testEvaluate_noError_unknown_property_in_check_details() {
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.REQUEST_UNKNOWN_PROPERTY_IN_CHECK_DETAILS));
	}

	@Test
	public void testEvaluate_noError_vouch_can_contain_document_details_without_document_branch_validation() {
		// Fields from a non-matching evidence branch are unevaluated properties; they are
		// reported (as a warning) by CheckForUnexpectedPropertiesInVerifiedClaimsRequest,
		// not by this condition.
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.REQUEST_WRONG_BRANCH_FIELD_ON_VOUCH_EVIDENCE));
	}

	@Test
	public void testEvaluate_noError_electronic_signature_branch_with_valid_date_time() {
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {
			          "given_name": null
			        },
			        "verification": {
			          "trust_framework": {
			            "value": "de_aml"
			          },
			          "evidence": [
			            {
			              "type": {
			                "value": "electronic_signature"
			              },
			              "created_at": {
			                "max_age": 0
			              }
			            }
			          ]
			        }
			      }
			    }
			  }
			}
			""";

		assertDoesNotThrow(() -> runTest(request));
	}

	@Test
	public void testEvaluate_fail_electronic_signature_branch_enforced() {
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {
			          "given_name": null
			        },
			        "verification": {
			          "trust_framework": {
			            "value": "de_aml"
			          },
			          "evidence": [
			            {
			              "type": {
			                "value": "electronic_signature"
			              },
			              "created_at": "2024-01-01"
			            }
			          ]
			        }
			      }
			    }
			  }
			}
			""";

		assertThrows(ConditionError.class, () -> runTest(request));
	}

	@Test
	public void testEvaluate_claimsNotAnObjectIsAConditionErrorNotACrash() {
		assertThrows(ConditionError.class, () -> runTest("""
			{"claims": "not-an-object"}
			"""));
	}
}
