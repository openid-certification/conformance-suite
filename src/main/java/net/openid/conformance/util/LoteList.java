package net.openid.conformance.util;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

import java.util.List;

/**
 * The lists of trusted entities the suite publishes about itself (see {@link LoteBuilder}): one
 * per EU profile of ETSI TS 119 602 (Annexes D to G), each listing the suite CA whose leaves sign
 * what the list is about under the service types the profile defines. Each CA appears under both an
 * issuance and a revocation service, as the eIDAS Dashboard asks of every notified provider; the
 * same certificate serves both, which the Dashboard allows for services of different types. An mDL providers list is added once the
 * organisers of an event publish the list and service type URIs for it, which the specification
 * does not define.
 */
public enum LoteList {

	PID_PROVIDERS("pid-providers", "http://uri.etsi.org/19602/LoTEType/EUPIDProvidersList",
		"http://uri.etsi.org/19602/PIDProvidersList/StatusDetn/EU", "http://uri.etsi.org/19602/PIDProviders/schemerules/EU",
		"EU PID Providers List", List.of(
			new Service("http://uri.etsi.org/19602/SvcType/PID/Issuance", "OpenID Foundation conformance suite PID issuer (mdoc)",
				TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT),
			new Service("http://uri.etsi.org/19602/SvcType/PID/Revocation", "OpenID Foundation conformance suite PID revocation (mdoc)",
				TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT),
			new Service("http://uri.etsi.org/19602/SvcType/PID/Issuance", "OpenID Foundation conformance suite PID issuer (SD-JWT VC)",
				TestKeysAndCerts.SuiteCa.PID_PROVIDER),
			new Service("http://uri.etsi.org/19602/SvcType/PID/Revocation", "OpenID Foundation conformance suite PID revocation (SD-JWT VC)",
				TestKeysAndCerts.SuiteCa.PID_PROVIDER))),

	WALLET_PROVIDERS("wallet-providers", "http://uri.etsi.org/19602/LoTEType/EUWalletProvidersList",
		"http://uri.etsi.org/19602/WalletProvidersList/StatusDetn/EU", "http://uri.etsi.org/19602/WalletProvidersList/schemerules/EU",
		"EU Wallet Providers List", List.of(
			new Service("http://uri.etsi.org/19602/SvcType/WalletSolution/Issuance", "OpenID Foundation conformance suite wallet",
				TestKeysAndCerts.SuiteCa.WALLET_PROVIDER),
			new Service("http://uri.etsi.org/19602/SvcType/WalletSolution/Revocation", "OpenID Foundation conformance suite wallet revocation",
				TestKeysAndCerts.SuiteCa.WALLET_PROVIDER))),

	WRPAC_PROVIDERS("wrpac-providers", "http://uri.etsi.org/19602/LoTEType/EUWRPACProvidersList",
		"http://uri.etsi.org/19602/WRPACProvidersList/StatusDetn/EU", "http://uri.etsi.org/19602/WRPACProvidersList/schemerules/EU",
		"EU WRPAC Providers List", List.of(
			new Service("http://uri.etsi.org/19602/SvcType/WRPAC/Issuance", "OpenID Foundation conformance suite access CA",
				TestKeysAndCerts.SuiteCa.ACCESS),
			new Service("http://uri.etsi.org/19602/SvcType/WRPAC/Revocation", "OpenID Foundation conformance suite access CA revocation",
				TestKeysAndCerts.SuiteCa.ACCESS))),

	WRPRC_PROVIDERS("wrprc-providers", "http://uri.etsi.org/19602/LoTEType/EUWRPRCProvidersList",
		"http://uri.etsi.org/19602/WRPRCProvidersList/StatusDetn/EU", "http://uri.etsi.org/19602/WRPRCProvidersList/schemerules/EU",
		"EU WRPRC Providers List", List.of(
			new Service("http://uri.etsi.org/19602/SvcType/WRPRC/Issuance", "OpenID Foundation conformance suite registration certificates",
				TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER),
			new Service("http://uri.etsi.org/19602/SvcType/WRPRC/Revocation", "OpenID Foundation conformance suite registration certificate status",
				TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER)));

	/** One trusted entity service: a service type and the CA whose certificate is its digital identity. */
	@Immutable
	public record Service(String serviceType, String name, TestKeysAndCerts.SuiteCa ca) {
	}

	/** The name in the list's URL: {@code /lote/{slug}.jwt}. */
	public final String slug;
	public final String loteType;
	public final String statusDeterminationApproach;
	public final String schemeTypeCommunityRules;
	public final String schemeName;
	public final ImmutableList<Service> services;

	LoteList(String slug, String loteType, String statusDeterminationApproach, String schemeTypeCommunityRules,
			String schemeName, List<Service> services) {
		this.slug = slug;
		this.loteType = loteType;
		this.statusDeterminationApproach = statusDeterminationApproach;
		this.schemeTypeCommunityRules = schemeTypeCommunityRules;
		this.schemeName = schemeName;
		this.services = ImmutableList.copyOf(services);
	}

	/** The list served under {@code slug}, or null when there is none. */
	public static LoteList bySlug(String slug) {
		for (LoteList list : values()) {
			if (list.slug.equals(slug)) {
				return list;
			}
		}
		return null;
	}
}
