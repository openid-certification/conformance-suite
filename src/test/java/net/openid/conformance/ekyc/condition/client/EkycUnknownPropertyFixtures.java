package net.openid.conformance.ekyc.condition.client;

/**
 * Payloads containing unknown/wrong-branch properties, shared between the structural-validator
 * tests and the unknown-property condition tests. Each payload is deliberately asserted both
 * ways - the structural validator (called with FAILURE) must NOT fail on it, while the
 * CheckForUnexpectedProperties* condition (called with WARNING) must flag it - so both sides
 * reference the same constant to keep that pairing in lockstep.
 */
final class EkycUnknownPropertyFixtures {

	/** Authorization request with an unknown property inside document_details. */
	static final String REQUEST_UNKNOWN_PROPERTY_IN_DOCUMENT_DETAILS = """
		{
		  "claims": {
		    "id_token": {
		      "verified_claims": {
		        "claims": {"given_name": null},
		        "verification": {
		          "trust_framework": {"value": "de_aml"},
		          "evidence": [{
		            "type": {"value": "document"},
		            "document_details": {
		              "type": null,
		              "personal_number": null
		            }
		          }]
		        }
		      }
		    }
		  }
		}
		""";

	/** Authorization request with an unknown property inside a check_details entry. */
	static final String REQUEST_UNKNOWN_PROPERTY_IN_CHECK_DETAILS = """
		{
		  "claims": {
		    "id_token": {
		      "verified_claims": {
		        "claims": {"given_name": null},
		        "verification": {
		          "trust_framework": {"value": "de_aml"},
		          "evidence": [{
		            "type": {"value": "document"},
		            "check_details": [{
		              "check_method": null,
		              "unknown_field": null
		            }]
		          }]
		        }
		      }
		    }
		  }
		}
		""";

	/**
	 * Authorization request with a document-branch field on vouch evidence. The evidence object
	 * uses allOf with if/then for conditional properties, so fields from non-matching branches
	 * are unevaluated properties.
	 */
	static final String REQUEST_WRONG_BRANCH_FIELD_ON_VOUCH_EVIDENCE = """
		{
		  "claims": {
		    "id_token": {
		      "verified_claims": {
		        "claims": {"given_name": null},
		        "verification": {
		          "trust_framework": {"value": "de_aml"},
		          "evidence": [{
		            "type": {"value": "vouch"},
		            "document_details": "ignored-for-vouch"
		          }]
		        }
		      }
		    }
		  }
		}
		""";

	/** Response claims with an unknown property inside document_details. */
	static final String RESPONSE_UNKNOWN_PROPERTY_IN_DOCUMENT_DETAILS = """
		{
		  "claims": {"given_name": "Paula"},
		  "verification": {
		    "trust_framework": "de_aml",
		    "evidence": [{
		      "type": "document",
		      "document_details": {
		        "type": "idcard",
		        "personal_number": "unknown-extension-property"
		      }
		    }]
		  }
		}
		""";

	/** Response claims with an unknown property inside a vouch attestation voucher. */
	static final String RESPONSE_UNKNOWN_PROPERTY_IN_VOUCHER = """
		{
		  "claims": {"given_name": "Paula"},
		  "verification": {
		    "trust_framework": "de_aml",
		    "evidence": [{
		      "type": "vouch",
		      "attestation": {
		        "type": "written_attestation",
		        "voucher": {
		          "given_name": "should-use-name-not-given_name"
		        }
		      }
		    }]
		  }
		}
		""";

	/**
	 * Response claims with an unknown property inside an embedded attachment. The attachment
	 * oneOf's other (external) branch also rejects content_type/content as additional
	 * properties, but only the unknown member itself may be reported - the sibling branch's
	 * rejections are an artefact of the failed oneOf, not unknown properties.
	 */
	static final String RESPONSE_UNKNOWN_PROPERTY_IN_ATTACHMENT = """
		{
		  "claims": {"given_name": "Paula"},
		  "verification": {
		    "trust_framework": "de_aml",
		    "evidence": [{
		      "type": "document",
		      "attachments": [{
		        "content_type": "image/png",
		        "content": "aGVsbG8=",
		        "unknown_member": "x"
		      }]
		    }]
		  }
		}
		""";

	/**
	 * Response claims whose embedded attachment fails structurally (content_type carries
	 * parameters, forbidden by the schema's pattern). Asserted the opposite way round to the
	 * payloads above: the structural validator must fail it, and the
	 * CheckForUnexpectedProperties* condition must stay silent - the other oneOf branch's
	 * additionalProperties rejections of content_type/content are not unknown properties.
	 */
	static final String RESPONSE_ATTACHMENT_CONTENT_TYPE_WITH_PARAMETERS = """
		{
		  "claims": {"given_name": "Paula"},
		  "verification": {
		    "trust_framework": "de_aml",
		    "evidence": [{
		      "type": "document",
		      "attachments": [{
		        "content_type": "text/plain; charset=utf-8",
		        "content": "aGVsbG8="
		      }]
		    }]
		  }
		}
		""";

	/** Response claims whose embedded attachment content is line-wrapped base64; see above. */
	static final String RESPONSE_ATTACHMENT_CONTENT_WITH_LINE_BREAK = """
		{
		  "claims": {"given_name": "Paula"},
		  "verification": {
		    "trust_framework": "de_aml",
		    "evidence": [{
		      "type": "document",
		      "attachments": [{
		        "content_type": "image/png",
		        "content": "aGVs\\nbG8="
		      }]
		    }]
		  }
		}
		""";

	/** Response claims with a document-branch field on vouch evidence; see the request twin. */
	static final String RESPONSE_WRONG_BRANCH_FIELD_ON_VOUCH_EVIDENCE = """
		{
		  "claims": {"given_name": "Paula"},
		  "verification": {
		    "trust_framework": "de_aml",
		    "evidence": [{
		      "type": "vouch",
		      "document_details": "ignored-for-vouch"
		    }]
		  }
		}
		""";

	private EkycUnknownPropertyFixtures() {
	}
}
