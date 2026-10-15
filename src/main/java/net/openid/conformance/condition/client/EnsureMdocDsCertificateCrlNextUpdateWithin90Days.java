package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509CRL;
import java.time.Duration;

/**
 * Checks that the CRL fetched for an mdoc's document signer certificate sets nextUpdate at most
 * 90 days after thisUpdate. ISO/IEC 18013-5 B.2 requires an IACA to issue a new CRL at least
 * every 90 days, and Table B.10 says the next CRL is issued no later than nextUpdate, so a later
 * nextUpdate does not promise the cadence B.2 requires. B.2 constrains how often the IACA
 * issues CRLs rather than the field, and an IACA may issue a new CRL before nextUpdate, so one
 * CRL cannot show B.2 was broken; the caller decides the severity. A missing nextUpdate is
 * reported by {@link ValidateMdocDsCertificateCrl}.
 */
public class EnsureMdocDsCertificateCrlNextUpdateWithin90Days extends AbstractMdocDsCertificateCrlCondition {

	private static final Duration MAX_REISSUE_INTERVAL = Duration.ofDays(90);

	@Override
	@PreEnvironment(strings = { ENV_CRL })
	public Environment evaluate(Environment env) {
		X509CRL crl = parseCrl(env);
		if (crl.getNextUpdate() == null) {
			log("The CRL has no nextUpdate, so the interval to thisUpdate cannot be checked", args("uri", env.getString(ENV_URI)));
			return env;
		}
		Duration interval = Duration.between(crl.getThisUpdate().toInstant(), crl.getNextUpdate().toInstant());
		if (interval.compareTo(MAX_REISSUE_INTERVAL) > 0) {
			throw error("The CRL named by the document signer certificate sets nextUpdate more than 90 days after thisUpdate,"
					+ " so it does not promise the new CRL that ISO 18013-5 B.2 requires at least every 90 days",
				args("uri", env.getString(ENV_URI), "this_update", crl.getThisUpdate(), "next_update", crl.getNextUpdate(),
					"interval_days", interval.toDays()));
		}
		logSuccess("The CRL named by the document signer certificate sets nextUpdate at most 90 days after thisUpdate",
			args("uri", env.getString(ENV_URI), "this_update", crl.getThisUpdate(), "next_update", crl.getNextUpdate()));
		return env;
	}
}
