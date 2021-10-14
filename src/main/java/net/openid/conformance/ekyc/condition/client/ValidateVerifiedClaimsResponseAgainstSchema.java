package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ValidateVerifiedClaimsResponseAgainstSchema extends AbstractCondition {
	//TODO Reading the schema from file didn't work, fails in unit tests due to a conflict with spring boot dependencies
	public static final String VERIFIED_CLAIMS_12_JSON_SCHEMA = "{\n" +
		"  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
		"  \"$id\": \"https://openid.net/schemas/verified_claims-12.json\",\n" +
		"  \"definitions\": {\n" +
		"    \"date_type\": {\n" +
		"      \"type\": \"string\",\n" +
		"      \"pattern\": \"^(?:(?:(?:(?:(?:[1-9]\\\\d)(?:0[48]|[2468][048]|[13579][26])|(?:(?:[2468][048]|[13579][26])00))(\\\\/|-|\\\\.)(?:0?2\\\\1(?:29)))|(?:(?:[1-9]\\\\d{3})(\\\\/|-|\\\\.)(?:(?:(?:0?[13578]|1[02])\\\\2(?:31))|(?:(?:0?[13-9]|1[0-2])\\\\2(?:29|30))|(?:(?:0?[1-9])|(?:1[0-2]))\\\\2(?:0?[1-9]|1\\\\d|2[0-8])))))$\"\n" +
		"    },\n" +
		"    \"time_type\": {\n" +
		"      \"type\": \"string\",\n" +
		"      \"pattern\": \"^(?:[\\\\+-]?\\\\d{4}(?!\\\\d{2}\\\\b))(?:(-?)(?:(?:0[1-9]|1[0-2])(?:\\\\1(?:[12]\\\\d|0[1-9]|3[01]))?|W(?:[0-4]\\\\d|5[0-2])(?:-?[1-7])?|(?:00[1-9]|0[1-9]\\\\d|[12]\\\\d{2}|3(?:[0-5]\\\\d|6[1-6])))(?:[T\\\\s](?:(?:(?:[01]\\\\d|2[0-3])(?:(:?)[0-5]\\\\d)?|24\\\\:?00)(?:[\\\\.,]\\\\d+(?!:))?)?(?:\\\\2[0-5]\\\\d(?:[\\\\.,]\\\\d+)?)?(?:[zZ]|(?:[\\\\+-])(?:[01]\\\\d|2[0-3]):?(?:[0-5]\\\\d)?)?)?)?$\"\n" +
		"    },\n" +
		"    \"document_number\": {\n" +
		"      \"type\": \"string\"\n" +
		"    },\n" +
		"    \"document_details\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"type\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"document_number\": {\n" +
		"          \"$ref\": \"#/definitions/document_number\"\n" +
		"        },\n" +
		"        \"number\": {\n" +
		"          \"$ref\": \"#/definitions/document_number\"\n" +
		"        },\n" +
		"        \"personal_number\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"serial_number\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"date_of_issuance\": {\n" +
		"          \"$ref\": \"#/definitions/date_type\"\n" +
		"        },\n" +
		"        \"date_of_expiry\": {\n" +
		"          \"$ref\": \"#/definitions/date_type\"\n" +
		"        },\n" +
		"        \"issuer\": {\n" +
		"          \"type\": \"object\",\n" +
		"          \"properties\": {\n" +
		"            \"name\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"formatted\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"street_address\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"locality\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"region\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"postal_code\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"country\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"country_code\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"jurisdiction\": {\n" +
		"              \"type\": \"string\"\n" +
		"            }\n" +
		"          }\n" +
		"        }\n" +
		"      }\n" +
		"    },\n" +
		"    \"validation_method\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"type\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"policy\":{\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"procedure\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"status\": {\n" +
		"          \"type\": \"string\"\n" +
		"        }\n" +
		"      }\n" +
		"    },\n" +
		"    \"verification_method\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"type\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"policy\":{\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"procedure\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"status\": {\n" +
		"          \"type\": \"string\"\n" +
		"        }\n" +
		"      }\n" +
		"    },\n" +
		"    \"evidence\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"type\": {\n" +
		"          \"type\": \"string\",\n" +
		"          \"enum\": [\n" +
		"            \"electronic_signature\",\n" +
		"            \"id_document\",\n" +
		"            \"document\",\n" +
		"            \"electronic_record\",\n" +
		"            \"vouch\",\n" +
		"            \"utility_bill\"\n" +
		"          ]\n" +
		"        },\n" +
		"        \"attachments\": {\n" +
		"          \"$ref\": \"#/definitions/attachments\"\n" +
		"        }\n" +
		"      },\n" +
		"      \"required\": [\n" +
		"        \"type\"\n" +
		"      ],\n" +
		"      \"allOf\": [\n" +
		"        {\n" +
		"          \"if\": {\n" +
		"            \"properties\": {\n" +
		"              \"type\": {\n" +
		"                \"value\": \"electronic_signature\"\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"then\": {\n" +
		"            \"properties\": {\n" +
		"              \"signature_type\": {\n" +
		"                \"type\": \"string\"\n" +
		"              },\n" +
		"              \"issuer\": {\n" +
		"                \"type\": \"string\"\n" +
		"              },\n" +
		"              \"serial_number\": {\n" +
		"                \"type\": \"string\"\n" +
		"              },\n" +
		"              \"created_at\": {\n" +
		"                \"$ref\": \"#/definitions/time_type\"\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"else\": {}\n" +
		"        },\n" +
		"        {\n" +
		"          \"if\": {\n" +
		"            \"properties\": {\n" +
		"              \"type\": {\n" +
		"                \"pattern\": \"^(id_)*document$\"\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"then\": {\n" +
		"            \"properties\": {\n" +
		"              \"validation_method\": {\n" +
		"                \"$ref\": \"#/definitions/validation_method\"\n" +
		"              },\n" +
		"              \"verification_method\": {\n" +
		"                \"$ref\": \"#/definitions/verification_method\"\n" +
		"              },\n" +
		"              \"method\": {\n" +
		"                \"type\": \"string\"\n" +
		"              },\n" +
		"              \"verifier\": {\n" +
		"                \"type\": \"object\",\n" +
		"                \"properties\": {\n" +
		"                  \"organization\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"txn\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  }\n" +
		"                }\n" +
		"              },\n" +
		"              \"time\": {\n" +
		"                \"$ref\": \"#/definitions/time_type\"\n" +
		"              },\n" +
		"              \"document_details\": {\n" +
		"                \"$ref\": \"#/definitions/document_details\"\n" +
		"              },\n" +
		"              \"document\": {\n" +
		"                \"$ref\": \"#/definitions/document_details\"\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"else\": {}\n" +
		"        },\n" +
		"        {\n" +
		"          \"if\": {\n" +
		"            \"properties\": {\n" +
		"              \"type\": {\n" +
		"                \"value\": \"electronic_record\"\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"then\": {\n" +
		"            \"properties\": {\n" +
		"              \"validation_method\": {\n" +
		"                \"$ref\": \"#/definitions/validation_method\"\n" +
		"              },\n" +
		"              \"verification_method\": {\n" +
		"                \"$ref\": \"#/definitions/verification_method\"\n" +
		"              },\n" +
		"              \"verifier\": {\n" +
		"                \"type\": \"object\",\n" +
		"                \"properties\": {\n" +
		"                  \"organization\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"txn\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  }\n" +
		"                }\n" +
		"              },\n" +
		"              \"time\": {\n" +
		"                \"$ref\": \"#/definitions/time_type\"\n" +
		"              },\n" +
		"              \"record\": {\n" +
		"                \"type\": \"object\",\n" +
		"                \"properties\": {\n" +
		"                  \"type\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"personal_number\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"created_at\": {\n" +
		"                    \"$ref\": \"#/definitions/date_type\"\n" +
		"                  },\n" +
		"                  \"date_of_expiry\": {\n" +
		"                    \"$ref\": \"#/definitions/date_type\"\n" +
		"                  },\n" +
		"                  \"source\": {\n" +
		"                    \"type\": \"object\",\n" +
		"                    \"properties\": {\n" +
		"                      \"name\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"formatted\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"street_address\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"locality\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"region\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"postal_code\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"country\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"country_code\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"jurisdiction\": {\n" +
		"                         \"type\": \"string\"\n" +
		"                      }\n" +
		"                    }\n" +
		"                  }\n" +
		"                }\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"else\": {}\n" +
		"        },\n" +
		"        {\n" +
		"          \"if\": {\n" +
		"            \"properties\": {\n" +
		"              \"type\": {\n" +
		"                \"value\": \"vouch\"\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"then\": {\n" +
		"            \"properties\": {\n" +
		"              \"validation_method\": {\n" +
		"                \"$ref\": \"#/definitions/validation_method\"\n" +
		"              },\n" +
		"              \"verification_method\": {\n" +
		"                \"$ref\": \"#/definitions/verification_method\"\n" +
		"              },\n" +
		"              \"verifier\": {\n" +
		"                \"type\": \"object\",\n" +
		"                \"properties\": {\n" +
		"                  \"organization\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"txn\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  }\n" +
		"                }\n" +
		"              },\n" +
		"              \"time\": {\n" +
		"                \"$ref\": \"#/definitions/time_type\"\n" +
		"              },\n" +
		"              \"attestation\": {\n" +
		"                \"type\": \"object\",\n" +
		"                \"properties\": {\n" +
		"                  \"type\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"reference_number\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"personal_number\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"date_of_issuance\": {\n" +
		"                    \"$ref\": \"#/definitions/date_type\"\n" +
		"                  },\n" +
		"                  \"date_of_expiry\": {\n" +
		"                    \"$ref\": \"#/definitions/date_type\"\n" +
		"                  },\n" +
		"                  \"voucher\": {\n" +
		"                    \"type\": \"object\",\n" +
		"                    \"properties\": {\n" +
		"                      \"name\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"birthdate\": {\n" +
		"                        \"$ref\": \"#/definitions/date_type\"\n" +
		"                      },\n" +
		"                      \"formatted\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"street_address\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"locality\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"region\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"postal_code\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"country\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"occupation\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      },\n" +
		"                      \"organization\": {\n" +
		"                        \"type\": \"string\"\n" +
		"                      }\n" +
		"                    }\n" +
		"                  }\n" +
		"                }\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"else\": {}\n" +
		"        },\n" +
		"        {\n" +
		"          \"if\": {\n" +
		"            \"properties\": {\n" +
		"              \"type\": {\n" +
		"                \"value\": \"utility_bill\"\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"then\": {\n" +
		"            \"properties\": {\n" +
		"              \"provider\": {\n" +
		"                \"type\": \"object\",\n" +
		"                \"properties\": {\n" +
		"                  \"name\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"formatted\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"street_address\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"locality\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"region\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"postal_code\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  },\n" +
		"                  \"country\": {\n" +
		"                    \"type\": \"string\"\n" +
		"                  }\n" +
		"                }\n" +
		"              },\n" +
		"              \"date\": {\n" +
		"                \"$ref\": \"#/definitions/date_type\"\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"else\": {}\n" +
		"        }\n" +
		"      ]\n" +
		"    },\n" +
		"    \"digest\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"alg\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"value\": {\n" +
		"          \"type\": \"string\",\n" +
		"          \"contentEncoding\": \"base64\"\n" +
		"        }\n" +
		"      },\n" +
		"      \"required\": [\n" +
		"        \"alg\",\n" +
		"        \"value\"\n" +
		"      ]\n" +
		"    },\n" +
		"    \"external_attachment\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"desc\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"digest\": {\n" +
		"          \"$ref\": \"#/definitions/digest\"\n" +
		"        },\n" +
		"        \"url\": {\n" +
		"          \"type\": \"string\",\n" +
		"          \"format\": \"uri\"\n" +
		"        },\n" +
		"        \"access_token\": {\n" +
		"          \"type\": [\"string\", \"null\"]\n" +
		"        },\n" +
		"        \"expires_in\": {\n" +
		"          \"type\": \"integer\",\n" +
		"          \"minimum\": 1\n" +
		"        }\n" +
		"      },\n" +
		"      \"required\": [\n" +
		"        \"digest\",\n" +
		"        \"url\"\n" +
		"      ]\n" +
		"    },\n" +
		"    \"embedded_attachment\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"desc\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"content_type\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"content\": {\n" +
		"          \"type\": \"string\",\n" +
		"          \"contentEncoding\": \"base64\"\n" +
		"        }\n" +
		"      },\n" +
		"      \"required\": [\n" +
		"        \"content_type\",\n" +
		"        \"content\"\n" +
		"      ]\n" +
		"    },\n" +
		"    \"attachments\": {\n" +
		"      \"type\": \"array\",\n" +
		"      \"minItems\": 1,\n" +
		"      \"items\": {\n" +
		"        \"oneOf\": [\n" +
		"          {\n" +
		"            \"$ref\": \"#/definitions/external_attachment\"\n" +
		"          },\n" +
		"          {\n" +
		"            \"$ref\": \"#/definitions/embedded_attachment\"\n" +
		"          }\n" +
		"        ]\n" +
		"      }\n" +
		"    },\n" +
		"    \"verified_claims_def\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"verification\": {\n" +
		"          \"type\": \"object\",\n" +
		"          \"properties\": {\n" +
		"            \"trust_framework\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"assurance_level\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"assurance_process\": {\n" +
		"              \"type\": \"object\",\n" +
		"              \"properties\": {\n" +
		"                \"policy\":{\n" +
		"                  \"type\": \"string\"\n" +
		"                },\n" +
		"                \"procedure\": {\n" +
		"                  \"type\": \"string\"\n" +
		"                },\n" +
		"                \"status\": {\n" +
		"                  \"type\": \"string\"\n" +
		"                }\n" +
		"              }\n" +
		"            },\n" +
		"            \"time\": {\n" +
		"              \"$ref\": \"#/definitions/time_type\"\n" +
		"            },\n" +
		"            \"verification_process\": {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            \"evidence\": {\n" +
		"              \"type\": \"array\",\n" +
		"              \"minItems\": 1,\n" +
		"              \"items\": {\n" +
		"                \"oneOf\": [\n" +
		"                  {\n" +
		"                    \"$ref\": \"#/definitions/evidence\"\n" +
		"                  }\n" +
		"                ]\n" +
		"              }\n" +
		"            }\n" +
		"          },\n" +
		"          \"required\": [\n" +
		"            \"trust_framework\"\n" +
		"          ],\n" +
		"          \"additionalProperties\": true\n" +
		"        },\n" +
		"        \"claims\": {\n" +
		"          \"type\": \"object\",\n" +
		"          \"minProperties\": 1\n" +
		"        }\n" +
		"      },\n" +
		"      \"required\": [\n" +
		"        \"verification\",\n" +
		"        \"claims\"\n" +
		"      ],\n" +
		"      \"additionalProperties\": false\n" +
		"    },\n" +
		"    \"distributed_claims_available_def\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {},\n" +
		"      \"additionalProperties\": {\n" +
//TODO the following line caused an exception so I commented it out as a workaround but the error should be fixed
//		"        \"$ref\": \"verified_claims_request-12.json#definitions/verified_claims_def\"\n" +
		"      }\n" +
		"    },\n" +
		"    \"aggregated_claims\": {\n" +
		"      \"properties\": {\n" +
		"        \"JWT\": {\n" +
		"          \"type\": \"string\"\n" +
		"        }\n" +
		"      },\n" +
		"      \"required\": [\n" +
		"        \"JWT\"\n" +
		"      ]\n" +
		"    },\n" +
		"    \"distributed_claims\": {\n" +
		"      \"properties\": {\n" +
		"        \"endpoint\": {\n" +
		"          \"type\": \"string\"\n" +
		"        },\n" +
		"        \"access_token\": {\n" +
		"          \"type\": \"string\"\n" +
		"        }\n" +
		"      },\n" +
		"      \"required\": [\n" +
		"        \"endpoint\",\n" +
		"        \"access_token\"\n" +
		"      ]\n" +
		"    },\n" +
		"    \"_claim_sources\": {\n" +
		"      \"anyOf\": [\n" +
		"        {\n" +
		"          \"$ref\": \"#/definitions/aggregated_claims\"\n" +
		"        },\n" +
		"        {\n" +
		"          \"$ref\": \"#/definitions/distributed_claims\"\n" +
		"        }\n" +
		"      ]\n" +
		"    }\n" +
		"  },\n" +
		"  \"properties\": {\n" +
		"    \"verified_claims\": {\n" +
		"      \"anyOf\": [\n" +
		"        {\n" +
		"          \"$ref\": \"#/definitions/verified_claims_def\"\n" +
		"        },\n" +
		"        {\n" +
		"          \"type\": \"array\",\n" +
		"          \"items\": {\n" +
		"            \"$ref\": \"#/definitions/verified_claims_def\"\n" +
		"          }\n" +
		"        }\n" +
		"      ]\n" +
		"    },\n" +
		"    \"_claim_names\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {\n" +
		"        \"verified_claims\": {\n" +
		"          \"anyOf\": [\n" +
		"            {\n" +
		"              \"type\": \"string\"\n" +
		"            },\n" +
		"            {\n" +
		"              \"type\": \"array\",\n" +
		"              \"items\": {\n" +
		"                \"type\": \"string\"\n" +
		"              }\n" +
		"            },\n" +
		"            {\n" +
		"              \"$ref\": \"#/definitions/distributed_claims_available_def\"\n" +
		"            }\n" +
		"          ]\n" +
		"        }\n" +
		"      },\n" +
		"      \"additionalProperties\": false\n" +
		"    },\n" +
		"    \"_claim_sources\": {\n" +
		"      \"type\": \"object\",\n" +
		"      \"properties\": {},\n" +
		"      \"additionalProperties\": {\n" +
		"        \"$ref\": \"#/definitions/_claim_sources\"\n" +
		"      }\n" +
		"    }\n" +
		"  }\n" +
		"}";
	@Override
	@PreEnvironment(required = {"verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonObject verifiedClaimsResponse = env.getObject("verified_claims_response");
		JsonElement claimsElement = null;
		String location = "";
		//TODO I assumed id_token will be processed before userinfo so if we have userinfo then just process it
		// otherwise process id_token
		if(verifiedClaimsResponse.has("userinfo")) {
			claimsElement = verifiedClaimsResponse.get("userinfo");
			location = "userinfo";
		} else {
			claimsElement = verifiedClaimsResponse.get("id_token");
			location = "id_token";
		}
		if(claimsElement==null) {
			throw error("Could not find verified_claims");
		}
		//we add the outer {"verified_claims":...} here
		String claimsJson = "{\"verified_claims\":" + claimsElement.toString() + "}";
		try {
			checkSchema(claimsJson);
		} catch (ValidationException ex) {
			throw error("Failed to validate verified_claims against schema", ex,
				args("verified_claims", claimsJson,
					"errors", new JsonParser().parse(ex.toJSON().toString())));
		}
		logSuccess("Verified claims are valid", args("location", location, "verified_claims", claimsElement));
		return env;
	}

	protected void checkSchema(String verifiedClaimsJson) {
		/*
		Reading the schema from file didn't work, fails in unit tests due to a conflict with spring boot dependencies
		throws 'java.lang.NoSuchMethodError: org.json.JSONTokener.<init>(Ljava/io/InputStream;)V'
		InputStream inputStream = null;
		if(isUnitTest==null || !isUnitTest) {
			inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("json-schemas/ekyc/verified_claims-11.json");
		} else {
			inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("src/main/resources/json-schemas/ekyc/verified_claims-11.json");
		}
		 */
		JSONObject jsonSchema = new JSONObject(new JSONTokener(VERIFIED_CLAIMS_12_JSON_SCHEMA));
		JSONObject jsonSubject = new JSONObject(new JSONTokener(verifiedClaimsJson));
		Schema schema = SchemaLoader.load(jsonSchema);
		schema.validate(jsonSubject);

	}

}
