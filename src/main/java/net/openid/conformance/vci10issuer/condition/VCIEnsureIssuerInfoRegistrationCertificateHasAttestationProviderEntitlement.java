package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks that the {@code entitlements} of the registration certificate in {@code issuer_info} include
 * at least one WRP entitlement of ETSI TS 119 475 clause A.2, and one of those that make the subject a
 * provider of attestations: PID_Provider, QEAA_Provider, Non_Q_EAA_Provider or PUB_EAA_Provider.
 */
public class VCIEnsureIssuerInfoRegistrationCertificateHasAttestationProviderEntitlement extends AbstractCondition {

	private static final String ENTITLEMENT_PREFIX = "https://uri.etsi.org/19475/Entitlement/";

	public static final Set<String> ATTESTATION_PROVIDER_ENTITLEMENTS = Set.of(
		ENTITLEMENT_PREFIX + "PID_Provider",
		ENTITLEMENT_PREFIX + "QEAA_Provider",
		ENTITLEMENT_PREFIX + "Non_Q_EAA_Provider",
		ENTITLEMENT_PREFIX + "PUB_EAA_Provider");

	private static final Set<String> WRP_ENTITLEMENTS = Set.of(
		ENTITLEMENT_PREFIX + "Service_Provider",
		ENTITLEMENT_PREFIX + "QEAA_Provider",
		ENTITLEMENT_PREFIX + "Non_Q_EAA_Provider",
		ENTITLEMENT_PREFIX + "PUB_EAA_Provider",
		ENTITLEMENT_PREFIX + "PID_Provider",
		ENTITLEMENT_PREFIX + "QCert_for_ESeal_Provider",
		ENTITLEMENT_PREFIX + "QCert_for_ESig_Provider",
		ENTITLEMENT_PREFIX + "rQSealCDs_Provider",
		ENTITLEMENT_PREFIX + "rQSigCDs_Provider",
		ENTITLEMENT_PREFIX + "ESig_ESeal_Creation_Provider");

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject payload = env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "payload").getAsJsonObject();

		JsonElement entitlementsEl = payload.get("entitlements");
		if (entitlementsEl == null || !entitlementsEl.isJsonArray() || entitlementsEl.getAsJsonArray().isEmpty()) {
			throw error("The registration certificate in 'issuer_info' has no non-empty 'entitlements' array",
				args("payload", payload));
		}

		List<String> entitlements = new ArrayList<>();
		for (JsonElement entitlement : entitlementsEl.getAsJsonArray()) {
			if (!entitlement.isJsonPrimitive() || !entitlement.getAsJsonPrimitive().isString()) {
				throw error("The 'entitlements' of the registration certificate in 'issuer_info' contain a value that is not a string",
					args("entitlements", entitlementsEl));
			}
			entitlements.add(OIDFJSON.getString(entitlement));
		}

		if (entitlements.stream().noneMatch(WRP_ENTITLEMENTS::contains)) {
			throw error("The 'entitlements' of the registration certificate in 'issuer_info' contain none of the wallet-relying"
					+ " party entitlement URIs", args("entitlements", entitlements, "expected_one_of", WRP_ENTITLEMENTS));
		}
		if (entitlements.stream().noneMatch(ATTESTATION_PROVIDER_ENTITLEMENTS::contains)) {
			throw error("The 'entitlements' of the registration certificate in 'issuer_info' do not register the issuer as a provider"
					+ " of attestations", args("entitlements", entitlements, "expected_one_of", ATTESTATION_PROVIDER_ENTITLEMENTS));
		}

		logSuccess("The registration certificate in 'issuer_info' registers the issuer as a provider of attestations",
			args("entitlements", entitlements));
		return env;
	}
}
