package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VCICredentialConfigurations {

	public static final String DEFAULT_JSON_STRING = """
			{
				"eu.europa.ec.eudi.pid.1": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.attestation": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"attestation": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.jwt.keyattest": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ],
							 "key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID: JWT Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.attestation.keyattest": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"attestation": {
							 "proof_signing_alg_values_supported": [ "ES256" ],
							 "key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID: Attestation Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.jwt_and_attestation.keyattest": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ],
							 "key_attestations_required": {}
						},
						"attestation": {
							 "proof_signing_alg_values_supported": [ "ES256" ],
							 "key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID: JWT and Attestation Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.mdoc.1": {
					"format": "mso_mdoc",
					"doctype": "eu.europa.ec.eudi.pid.1",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (mdoc)",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.mdoc.1.attestation": {
					"format": "mso_mdoc",
					"doctype": "eu.europa.ec.eudi.pid.1",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"attestation": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (mdoc)",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.mdoc.1.jwt.keyattest": {
					"format": "mso_mdoc",
					"doctype": "eu.europa.ec.eudi.pid.1",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ],
							"key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (mdoc): JWT Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.mdoc.1.attestation.keyattest": {
					"format": "mso_mdoc",
					"doctype": "eu.europa.ec.eudi.pid.1",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"attestation": {
							"proof_signing_alg_values_supported": [ "ES256" ],
							"key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (mdoc): Attestation Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					}
				},
				"org.iso.18013.5.1.mDL": {
					"format": "mso_mdoc",
					"doctype": "org.iso.18013.5.1.mDL",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake mDL (ISO 18013-5)",
							"description": "OpenID Conformance Test Fake Mobile Driver's License"
						}
						]
					}
				},
				"org.iso.18013.5.1.mDL.attestation": {
					"format": "mso_mdoc",
					"doctype": "org.iso.18013.5.1.mDL",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"attestation": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake mDL (ISO 18013-5)",
							"description": "OpenID Conformance Test Fake Mobile Driver's License"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.nobinding": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"credential_signing_alg_values_supported": [ "ES256" ],
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (No Holder Binding)",
							"description": "OpenID Conformance Test Fake PID without cryptographic holder binding"
						}
						]
					}
				}
			}
		""";

	public static JsonObject getDefault() {
		return JsonParser.parseString(DEFAULT_JSON_STRING).getAsJsonObject();
	}
}
