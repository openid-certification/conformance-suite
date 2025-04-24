package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.ValidationMessage;
import com.tananaev.jsonpatch.JsonPatch;
import com.tananaev.jsonpatch.JsonPatchFactory;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
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
import java.util.Set;

import static net.openid.conformance.ekyc.condition.client.AbstractValidateAgainstSchema.checkRequestSchema;
import static net.openid.conformance.ekyc.condition.client.AbstractValidateAgainstSchema.checkResponseSchema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(MockitoExtension.class)
public class AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	protected void runTestWithStringData(String jsonRequestData, String jsonResponseData) throws IOException {
		env.putObjectFromJsonString("config", "ekyc.userinfo", jsonRequestData);
		env.putObject("authorization_endpoint_request", new JsonObject());
		JsonObject expected = (JsonObject) JsonParser.parseString(jsonResponseData);

		Set<ValidationMessage> errors = checkRequestSchema(jsonResponseData);
		if (!errors.isEmpty()) {
			for (ValidationMessage error: errors) {
				System.out.println("RequestJsonSchemaError: " + error.toString());
			}
		}

		errors = checkResponseSchema(jsonResponseData);
		if (!errors.isEmpty()) {
			for (ValidationMessage error: errors) {
				System.out.println("expected ResponseJsonSchemaError: " + error.toString());
			}
		}

		cond.execute(env);
		JsonObject result = env.getObject("authorization_endpoint_request");

		errors = checkRequestSchema(result.get("claims").toString());
		if (!errors.isEmpty()) {
			for (ValidationMessage error: errors) {
				System.out.println("RequestJsonSchemaError: " + error.toString());
			}
		}
		assertThat(errors.size()).isEqualTo(0);


		JsonPatchFactory jpf = new JsonPatchFactory();
		JsonPatch patch = jpf.create(expected, result);

		System.out.println("patch: " + patch.toString());
		assertThat(patch.size()).isEqualTo(0);
	}

	protected void runTest(String userInfoFilename, String expectedRequestFilename) throws IOException {
		String testUserInfoJson = IOUtils.resourceToString(userInfoFilename, StandardCharsets.UTF_8, getClass().getClassLoader());
		String expectedJson = IOUtils.resourceToString(expectedRequestFilename, StandardCharsets.UTF_8, getClass().getClassLoader());

		runTestWithStringData(testUserInfoJson, expectedJson);
	}

	@Test
	public void testEvaluate_noError() throws Exception {
		String userInfoFilename = "test-user-info.json";
		String expectedRequestFilename = "verified-claims-request-based-on-userinfo.json";

		runTest(userInfoFilename, expectedRequestFilename);
	}

	@Test
	public void testEvaluate_noErrorYesTest001() throws Exception {
		String userInfoFilename = "test-user-info-yes-test001.json";
		String expectedRequestFilename = "verified-claims-request-based-on-userinfo-yes-test001.json";

		runTest(userInfoFilename, expectedRequestFilename);
	}

	@Test
	public void testAssuranceProcess() throws Exception {
		String userInfoData = """
			{
				"sub": "f647f683-e46d-43bd-bc76-526d93429b86",
				"verified_claims": {
					"claims": {
						"birthdate": "1950-01-01"
					},
					"verification": {
						"trust_framework": "de_aml",
						"verification_process": "vp1",
						"assurance_process": {
							"policy": "policy1",
							"procedure": "procedure1"
						}
					}
				}
			}
			""";

		String expecteData = """
			{
			    "claims": {
			        "userinfo": {
			            "verified_claims": {
			                "claims": {
			                    "birthdate": null
			                },
			                "verification": {
			                    "trust_framework": {
			                        "value": "de_aml"
			                    },
			                    "verification_process": {
			                        "value": "vp1"
			                    },
			                    "assurance_process": {
			                        "policy": {
			                            "value": "policy1"
			                        },
			                        "procedure": {
			                            "value": "procedure1"
			                        }
			                    }
			                }
			            }
			        }
			    }
			}""";

		runTestWithStringData(userInfoData, expecteData);
	}


	@Test
	public void testAssuranceProcessAssuranceDetails() throws Exception {
		String userInfoData = """
			{
			    "sub": "f647f683-e46d-43bd-bc76-526d93429b86",
			    "verified_claims": {
			        "claims": {
			            "birthdate": "1950-01-01",
			            "given_name": "Given001",
			            "family_name": "Family001"
			        },
			        "verification": {
			            "evidence": [
			                {
			                    "method": "pipp",
			                    "type": "document"
			                },
			                {
			                    "method": "pipp",
			                    "type": "electronic_record"
			                }
			            ],
			            "trust_framework": "de_aml",
			            "verification_process": "vp1",
			            "assurance_process": {
			                "policy": "policy1",
			                "procedure": "procedure1",
			                "assurance_details": [
			                    {
			                        "assurance_type": "assurance_type1",
			                        "assurance_classification": "assurance_classification1",
			                        "evidence_ref": [
			                            {
			                                "check_id": "id1234",
			                                "evidence_metadata": {
			                                    "evidence_classification": "evc1"
			                                }
			                            }
			                        ]
			                    }
			                ]
			            }
			        }
			    }
			}""";

		String expecteData = """
			{
			    "claims": {
			      "userinfo": {
			        "verified_claims": {
			          "claims": {
			            "birthdate": null,
			            "given_name": null,
			            "family_name": null
			          },
			          "verification": {
			            "trust_framework": {
			              "value": "de_aml"
			            },
			            "verification_process": {
			              "value": "vp1"
			            },
			            "assurance_process": {
			              "policy": {
			                "value": "policy1"
			              },
			              "procedure": {
			                "value": "procedure1"
			              },
			              "assurance_details": [
			                {
			                  "assurance_type": {
			                    "value": "assurance_type1"
			                  },
			                  "assurance_classification": {
			                    "value": "assurance_classification1"
			                  },
			                  "evidence_ref": [
			                    {
			                      "check_id": {
			                        "value": "id1234"
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
			            },
			            "evidence": [
			              {
			                "type": {
			                  "value": "document"
			                }
			              },
			              {
			                "type": {
			                  "value": "electronic_record"
			                }
			              }
			            ]
			          }
			        }
			      }
			    }
			  }""";

		runTestWithStringData(userInfoData, expecteData);
	}

	@Test
	public void testAssuranceProcessAssuranceDetailsNoCheckId() throws Exception {
		assertThrows(ConditionError.class, () -> {
			String userInfoData = """
				{
				    "sub": "f647f683-e46d-43bd-bc76-526d93429b86",
				    "verified_claims": {
				        "claims": {
				            "birthdate": "1950-01-01",
				            "given_name": "Given001",
				            "family_name": "Family001"
				        },
				        "verification": {
				            "evidence": [
				                {
				                    "method": "pipp",
				                    "type": "document"
				                },
				                {
				                    "method": "pipp",
				                    "type": "electronic_record"
				                }
				            ],
				            "trust_framework": "de_aml",
				            "verification_process": "vp1",
				            "assurance_process": {
				                "policy": "policy1",
				                "procedure": "procedure1",
				                "assurance_details": [
				                    {
				                        "assurance_type": "assurance_type1",
				                        "assurance_classification": "assurance_classification1",
				                        "evidence_ref": [
				                            {
				                                "evidence_metadata": {
				                                    "evidence_classification": "evc1"
				                                }
				                            }
				                        ]
				                    }
				                ]
				            }
				        }
				    }
				}""";

			String expecteData = """
				{
				    "claims": {
				        "userinfo": {
				            "verified_claims": {
				                "claims": {
				                    "birthdate": null,
				                    "given_name": null,
				                    "family_name": null
				                },
				                "verification": {
				                    "trust_framework": {
				                        "value": "de_aml"
				                    },
				                    "verification_process": {
				                        "value": "vp1"
				                    },
				                    "assurance_process": {
				                        "policy": {
				                            "value": "policy1"
				                        },
				                        "procedure": {
				                            "value": "procedure1"
				                        },
				                        "assurance_details": [
				                            {
				                                "assurance_type": {
				                                    "value": "assurance_type1"
				                                },
				                                "assurance_classification": {
				                                    "value": "assurance_classification1"
				                                },
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
				                    },
				                    "evidence": [
				                        {
				                            "type": {
				                                "value": "document"
				                            }
				                        },
				                        {
				                            "type": {
				                                "value": "electronic_record"
				                            }
				                        }
				                    ]
				                }
				            }
				        }
				    }
				}""";

			runTestWithStringData(userInfoData, expecteData);
		});

	}


	@Test
	public void testDocumentEvidence() throws Exception {
			String userInfoData = """
				{
				    "sub": "f647f683-e46d-43bd-bc76-526d93429b86",
				    "verified_claims": {
				        "claims": {
				            "birthdate": "1950-01-01"
				        },
				        "verification": {
				            "trust_framework": "de_aml",
				            "verification_process": "vp1",
				            "assurance_process": {
				                "policy": "policy1",
				                "procedure": "procedure1",
				                "assurance_details": [
				                    {
				                        "assurance_type": "assurance_type1",
				                        "assurance_classification": "assurance_classification1",
				                        "evidence_ref": [
				                            {
				                                "check_id": "check_id2",
				                                "evidence_metadata": {
				                                    "evidence_classification": "evidence_classification1"
				                                }
				                            }
				                        ]
				                    }
				                ]
				            },
				            "evidence": [
				                {
				                    "type": "document",
				                    "check_details": [
				                        {
				                            "check_method": "checkMethod1",
				                            "organization": "myorg1",
				                            "check_id": "mycheckid1",
				                            "time": "2022-12-12T00:30Z"
				                        }
				                    ],
				                    "document_details": {
				                        "type": "idcard",
				                        "document_number": "docnum1234",
				                        "serial_number": "serial123",
				                        "date_of_issuance": "2020-01-01",
				                        "date_of_expiry": "2030-01-01",
				                        "issuer": {
				                            "name": "issuername",
				                            "country_code": "US",
				                            "jurisdiction": "CA",
				                            "street_address": "123 Example St.",
				                            "locality": "Hollywood",
				                            "region": "CA",
				                            "postal_code": "90210"
				                        }
				                    }
				                }
				            ]
				        }
				    }
				}""";

			String expecteData = """
				{
				    "claims": {
				        "userinfo": {
				            "verified_claims": {
				                "claims": {
				                    "birthdate": null
				                },
				                "verification": {
				                    "trust_framework": {
				                        "value": "de_aml"
				                    },
				                    "verification_process": {
				                        "value": "vp1"
				                    },
				                    "assurance_process": {
				                        "policy": {
				                            "value": "policy1"
				                        },
				                        "procedure": {
				                            "value": "procedure1"
				                        },
				                        "assurance_details": [
				                            {
				                                "assurance_type": {
				                                    "value": "assurance_type1"
				                                },
				                                "assurance_classification": {
				                                    "value": "assurance_classification1"
				                                },
				                                "evidence_ref": [
				                                    {
				                                        "check_id": {
				                                            "value": "check_id2"
				                                        },
				                                        "evidence_metadata": {
				                                            "evidence_classification": {
				                                                "value": "evidence_classification1"
				                                            }
				                                        }
				                                    }
				                                ]
				                            }
				                        ]
				                    },
				                    "evidence": [
				                        {
				                            "type": {
				                                "value": "document"
				                            },
				                            "check_details": [
				                                {
				                                    "check_method": {
				                                        "value": "checkMethod1"
				                                    },
				                                    "organization": {
				                                        "value": "myorg1"
				                                    },
				                                    "check_id": {
				                                        "value": "mycheckid1"
				                                    },
				                                    "time": {
				                                        "value": "2022-12-12T00:30Z"
				                                    }
				                                }
				                            ],
				                            "document_details": {
				                                "type": {
				                                    "value": "idcard"
				                                },
				                                "document_number": {
				                                    "value": "docnum1234"
				                                },
				                                "serial_number": {
				                                    "value": "serial123"
				                                },
				                                "date_of_issuance": {
				                                    "value": "2020-01-01"
				                                },
				                                "date_of_expiry": {
				                                    "value": "2030-01-01"
				                                },
				                                "issuer": {
				                                    "name": {
				                                        "value": "issuername"
				                                    },
				                                    "country_code": {
				                                        "value": "US"
				                                    },
				                                    "jurisdiction": {
				                                        "value": "CA"
				                                    },
				                                    "street_address": {
				                                        "value": "123 Example St."
				                                    },
				                                    "locality": {
				                                        "value": "Hollywood"
				                                    },
				                                    "region": {
				                                        "value": "CA"
				                                    },
				                                    "postal_code": {
				                                        "value": "90210"
				                                    }
				                                }
				                            }
				                        }
				                    ]
				                }
				            }
				        }
				    }
				}""";


			runTestWithStringData(userInfoData, expecteData);
	}


	@Test
	public void testElectronicRecordEvidence() throws Exception {
		String userInfoData = """
			{
			  "sub": "f647f683-e46d-43bd-bc76-526d93429b86",
			  "verified_claims": {
			    "claims": {
			      "birthdate": "1950-01-01"
			    },
			    "verification": {
			      "trust_framework": "de_aml",
			      "verification_process": "vp1",
			      "assurance_process": {
			        "policy": "policy1",
			        "procedure": "procedure1",
			        "assurance_details" : [
			          {
			            "assurance_type": "assurance_type1",
			            "assurance_classification": "assurance_classification1",
			            "evidence_ref": [
			              {
			                "check_id": "check_id2",
			                "evidence_metadata": {
			                  "evidence_classification": "evidence_classification1"
			                }
			              }
			            ]
			          }
			        ]
			      },
			      "evidence": [
			        {
			          "type": "electronic_record",
			          "check_details": [
			            {
			              "check_method": "checkMethod1",
			              "organization": "myorg1",
			              "check_id": "mycheckid1",
			              "time": "2022-12-12T00:30Z"
			            }
			          ],
			          "record": {
			            "type": "idcard",
			            "created_at": "2020-01-01T00:00Z",
			            "date_of_expiry": "2030-01-01",
			            "source": {
			              "name": "sourcename",
			              "country_code": "US",
			              "jurisdiction": "CA",
			              "street_address": "123 Example St.",
			              "locality": "Hollywood",
			              "region": "CA",
			              "postal_code": "90210"
			            }
			          }
			        }
			      ]
			    }
			  }
			}""";

		String expecteData = """
			  {
			    "claims": {
			      "userinfo": {
			        "verified_claims": {
			          "claims": {
			            "birthdate": null
			          },
			          "verification": {
			            "trust_framework": {
			              "value": "de_aml"
			            },
			            "verification_process": {
			              "value": "vp1"
			            },
			            "assurance_process": {
			              "policy": {
			                "value": "policy1"
			              },
			              "procedure": {
			                "value": "procedure1"
			              },
			              "assurance_details" : [
			                {
			                  "assurance_type": {
			                    "value": "assurance_type1"
			                  },
			                  "assurance_classification": {
			                    "value": "assurance_classification1"
			                  },
			                  "evidence_ref": [
			                    {
			                      "check_id": {
			                        "value": "check_id2"
			                      },
			                      "evidence_metadata": {
			                        "evidence_classification": {
			                          "value": "evidence_classification1"
			                        }
			                      }
			                    }
			                  ]
			                }
			              ]
			            },
			            "evidence": [
			              {
			                "type": {
			                  "value": "electronic_record"
			                },
			                "check_details": [
			                  {
			                    "check_method": {
			                      "value": "checkMethod1"
			                    },
			                    "organization": {
			                      "value": "myorg1"
			                    },
			                    "check_id": {
			                      "value": "mycheckid1"
			                    },
			                    "time": {
			                      "value": "2022-12-12T00:30Z"
			                    }
			                  }
			                ],
			                "record": {
			                  "type": {
			                    "value": "idcard"
			                  },
			                  "created_at": {
			                    "value": "2020-01-01T00:00Z"
			                  },
			                  "date_of_expiry": {
			                    "value": "2030-01-01"
			                  },
			                  "source": {
			                    "name": {
			                      "value": "sourcename"
			                    },
			                    "country_code": {
			                      "value": "US"
			                    },
			                    "jurisdiction": {
			                      "value": "CA"
			                    },
			                    "street_address": {
			                      "value": "123 Example St."
			                    },
			                    "locality": {
			                      "value": "Hollywood"
			                    },
			                    "region": {
			                      "value": "CA"
			                    },
			                    "postal_code": {
			                      "value": "90210"
			                    }
			                  }
			                }
			              }
			            ]
			          }
			        }
			      }
			    }
			  }""";

		runTestWithStringData(userInfoData, expecteData);
	}

	@Test
	public void testVouchEvidence() throws Exception {
		String userInfoData = """
			{
			  "sub": "f647f683-e46d-43bd-bc76-526d93429b86",
			  "verified_claims": {
			    "claims": {
			      "birthdate": "1950-01-01"
			    },
			    "verification": {
			      "trust_framework": "de_aml",
			      "verification_process": "vp1",
			      "assurance_process": {
			        "policy": "policy1",
			        "procedure": "procedure1",
			        "assurance_details" : [
			          {
			            "assurance_type": "assurance_type1",
			            "assurance_classification": "assurance_classification1",
			            "evidence_ref": [
			              {
			                "check_id": "check_id2",
			                "evidence_metadata": {
			                  "evidence_classification": "evidence_classification1"
			                }
			              }
			            ]
			          }
			        ]
			      },
			      "evidence": [
			        {
			          "type": "vouch",
			          "check_details": [
			            {
			              "check_method": "checkMethod1",
			              "organization": "myorg1",
			              "check_id": "mycheckid1",
			              "time": "2022-12-12T00:30Z"
			            }
			          ],
			          "attestation": {
			            "type": "written_attestation",
			            "reference_number": "ref12345",
			            "date_of_issuance": "2020-01-01",
			            "date_of_expiry": "2030-01-01",
			            "voucher": {
			              "name": "vouchname",
			              "country_code": "US",
			              "birthdate": "2010-01-01",
			              "occupation": "worker",
			              "jurisdiction": "CA",
			              "street_address": "123 Example St.",
			              "locality": "Hollywood",
			              "region": "CA",
			              "postal_code": "90210"
			            }
			          }
			        }
			      ]
			    }
			  }
			}""";

		String expecteData = """
			  {
			    "claims": {
			      "userinfo": {
			        "verified_claims": {
			          "claims": {
			            "birthdate": null
			          },
			          "verification": {
			            "trust_framework": {
			              "value": "de_aml"
			            },
			            "verification_process": {
			              "value": "vp1"
			            },
			            "assurance_process": {
			              "policy": {
			                "value": "policy1"
			              },
			              "procedure": {
			                "value": "procedure1"
			              },
			              "assurance_details" : [
			                {
			                  "assurance_type": {
			                    "value": "assurance_type1"
			                  },
			                  "assurance_classification": {
			                    "value": "assurance_classification1"
			                  },
			                  "evidence_ref": [
			                    {
			                      "check_id": {
			                        "value": "check_id2"
			                      },
			                      "evidence_metadata": {
			                        "evidence_classification": {
			                          "value": "evidence_classification1"
			                        }
			                      }
			                    }
			                  ]
			                }
			              ]
			            },
			            "evidence": [
			              {
			                "type": {
			                  "value": "vouch"
			                },
			                "check_details": [
			                  {
			                    "check_method": {
			                      "value": "checkMethod1"
			                    },
			                    "organization": {
			                      "value": "myorg1"
			                    },
			                    "check_id": {
			                      "value": "mycheckid1"
			                    },
			                    "time": {
			                      "value": "2022-12-12T00:30Z"
			                    }
			                  }
			                ],
			                "attestation": {
			                  "type": {
			                    "value": "written_attestation"
			                  },
			                  "reference_number": {
			                    "value": "ref12345"
			                  },
			                  "date_of_issuance": {
			                    "value": "2020-01-01"
			                  },
			                  "date_of_expiry": {
			                    "value": "2030-01-01"
			                  },
			                  "voucher": {
			                    "name": {
			                      "value": "vouchname"
			                    },
			                    "birthdate": {
			                      "value": "2010-01-01"
			                    },
			                    "country_code": {
			                      "value": "US"
			                    },
			                    "occupation": {
			                      "value": "worker"
			                    },
			                    "street_address": {
			                      "value": "123 Example St."
			                    },
			                    "locality": {
			                      "value": "Hollywood"
			                    },
			                    "region": {
			                      "value": "CA"
			                    },
			                    "postal_code": {
			                      "value": "90210"
			                    }
			                  }
			                }
			              }
			            ]
			          }
			        }
			      }
			    }
			  }""";

		runTestWithStringData(userInfoData, expecteData);
	}

	@Test
	public void testElectronicSignatureEvidence() throws Exception {
		String userInfoData = """
			{
			  "sub": "f647f683-e46d-43bd-bc76-526d93429b86",
			  "verified_claims": {
			    "claims": {
			      "birthdate": "1950-01-01"
			    },
			    "verification": {
			      "trust_framework": "de_aml",
			      "verification_process": "vp1",
			      "assurance_process": {
			        "policy": "policy1",
			        "procedure": "procedure1",
			        "assurance_details" : [
			          {
			            "assurance_type": "assurance_type1",
			            "assurance_classification": "assurance_classification1",
			            "evidence_ref": [
			              {
			                "check_id": "check_id2",
			                "evidence_metadata": {
			                  "evidence_classification": "evidence_classification1"
			                }
			              }
			            ]
			          }
			        ]
			      },
			      "evidence": [
			        {
			          "type": "electronic_signature",
			          "signature_type": "sigType1",
			          "issuer": "ESIssuer",
			          "serial_number": "Serial12345",
			          "created_at": "2023-01-01T00:00Z"
			        }
			      ]
			    }
			  }
			}""";

		String expecteData = """
			  {
			    "claims": {
			      "userinfo": {
			        "verified_claims": {
			          "claims": {
			            "birthdate": null
			          },
			          "verification": {
			            "trust_framework": {
			              "value": "de_aml"
			            },
			            "verification_process": {
			              "value": "vp1"
			            },
			            "assurance_process": {
			              "policy": {
			                "value": "policy1"
			              },
			              "procedure": {
			                "value": "procedure1"
			              },
			              "assurance_details" : [
			                {
			                  "assurance_type": {
			                    "value": "assurance_type1"
			                  },
			                  "assurance_classification": {
			                    "value": "assurance_classification1"
			                  },
			                  "evidence_ref": [
			                    {
			                      "check_id": {
			                        "value": "check_id2"
			                      },
			                      "evidence_metadata": {
			                        "evidence_classification": {
			                          "value": "evidence_classification1"
			                        }
			                      }
			                    }
			                  ]
			                }
			              ]
			            },
			            "evidence": [
			              {
			                "type": {
			                  "value": "electronic_signature"
			                },
			                "signature_type": {
			                  "value": "sigType1"
			                },
			                "issuer": {
			                  "value": "ESIssuer"
			                },
			                "serial_number": {
			                  "value": "Serial12345"
			                },
			                "created_at": {
			                  "value": "2023-01-01T00:00Z"
			                }
			              }
			            ]
			          }
			        }
			      }
			    }
			  }""";


		runTestWithStringData(userInfoData, expecteData);
	}

}
