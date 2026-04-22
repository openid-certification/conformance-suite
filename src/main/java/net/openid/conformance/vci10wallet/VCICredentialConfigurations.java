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
							"name": "Fake PID ${testId}",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					},
					"scope": "eudi.pid.1"
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
							"name": "Fake PID: Attestation ${testId}",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					},
					"scope": "eudi.pid.1.attestation"
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
							"name": "Fake PID: JWT Proof with Key Attestation ${testId}",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					},
					"scope": "eudi.pid.1.jwt.keyattest"
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
							"name": "Fake PID: Attestation Proof with Key Attestation ${testId}",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					},
					"scope": "eudi.pid.1.attestation.keyattest"
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
							"name": "Fake PID: JWT and Attestation Proof with Key Attestation ${testId}",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					},
					"scope": "eudi.pid.1.jwt_and_attestation.keyattest"
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
							"name": "Fake PID (mdoc) ${testId}",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					},
					"scope": "eudi.pid.mdoc.1"
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
							"name": "Fake PID (mdoc): Attestation ${testId}",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					},
					"scope": "eudi.pid.mdoc.1.attestation"
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
							"name": "Fake PID (mdoc): JWT Proof with Key Attestation ${testId}",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					},
					"scope": "eudi.pid.mdoc.1.jwt.keyattest"
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
							"name": "Fake PID (mdoc): Attestation Proof with Key Attestation ${testId}",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					},
					"scope": "eudi.pid.mdoc.1.attestation.keyattest"
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
							"name": "Fake mDL (ISO 18013-5) ${testId}",
							"description": "OpenID Conformance Test Fake Mobile Driver's License"
						}
						]
					},
					"scope": "org.iso.18013.5.1.mDL"
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
							"name": "Fake mDL (ISO 18013-5): Attestation ${testId}",
							"description": "OpenID Conformance Test Fake Mobile Driver's License"
						}
						]
					},
					"scope": "org.iso.18013.5.1.mDL.attestation"
				},
				"eu.europa.ec.eudi.pid.1.nobinding": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"credential_signing_alg_values_supported": [ "ES256" ],
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (No Holder Binding) ${testId}",
							"description": "OpenID Conformance Test Fake PID without cryptographic holder binding"
						}
						]
					},
					"scope": "eudi.pid.1.nobinding"
				},
				"net.openid.examples.certification.1.sdjwtvc": {
					"format": "dc+sd-jwt",
					"vct": "urn:openid:example:certification:1",
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
							"name": "Fake OpenID Certification ${testId}",
							"description": "OpenID Conformance Test Fake Certification"
						}
						]
					},
					"scope": "openid.example.cert.1"
				},
				"net.openid.examples.certification.1.mdoc": {
					"format": "mso_mdoc",
					"doctype": "net.openid.examples.certification.1.mdoc",
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
							"name": "Fake OpenID Certification (mdoc) ${testId}",
							"description": "OpenID Conformance Test Fake Certification"
						}
						]
					},
					"scope": "openid.example.cert.1"
				}
			}
		""";

	public static JsonObject getDefault(String testId) {
		String jsonString = DEFAULT_JSON_STRING.replace("${testId}", testId);
		return JsonParser.parseString(jsonString).getAsJsonObject();
	}
}
