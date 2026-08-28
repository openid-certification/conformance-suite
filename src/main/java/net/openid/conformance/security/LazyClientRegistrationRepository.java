package net.openid.conformance.security;

import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.function.Supplier;

/**
 * Resolves the IdP's client registration on first use rather than at startup.
 * <p>
 * Building a registration from an issuer location fetches the IdP's OIDC discovery
 * document. Doing that while the bean is created ties application startup to the
 * IdP being reachable: a momentary outage — one 503 from a cache in front of the
 * IdP is enough — stops the conformance suite from starting at all, including for
 * the many tests and API calls that never touch login.
 * <p>
 * A failed resolution is not remembered, so an outage heals on the next attempt
 * without a restart. When {@code tolerateFailure} is set (dev mode) a failure
 * yields no registration instead of propagating, matching the previous behaviour
 * of degrading rather than failing.
 * <p>
 * <strong>Deliberately not {@link Iterable}.</strong> Spring Security's
 * {@code OAuth2LoginConfigurer#getLoginLinks} type-checks the repository for
 * {@code Iterable<ClientRegistration>} and, if it matches, iterates it while the
 * filter chain is being built — at startup. Implementing Iterable here would
 * therefore resolve discovery eagerly again and silently undo this class. The
 * suite renders its own login page, so the generated login links are unused.
 */
public final class LazyClientRegistrationRepository implements ClientRegistrationRepository {

	private static final Logger logger = LoggerFactory.getLogger(LazyClientRegistrationRepository.class);

	private final Supplier<ClientRegistrationRepository> delegate;

	private final boolean tolerateFailure;

	/**
	 * @param delegate        resolves the registrations; may perform network I/O and
	 *                        may throw
	 * @param tolerateFailure when true, a resolution failure logs and yields no
	 *                        registration rather than propagating
	 */
	public LazyClientRegistrationRepository(Supplier<ClientRegistrationRepository> delegate, boolean tolerateFailure) {
		// Guava's memoizing supplier is thread safe and does not memoize a thrown
		// exception, so only a successful resolution is cached.
		this.delegate = Suppliers.memoize(delegate::get);
		this.tolerateFailure = tolerateFailure;
	}

	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		ClientRegistrationRepository resolved = resolve();
		return resolved == null ? null : resolved.findByRegistrationId(registrationId);
	}

	private ClientRegistrationRepository resolve() {
		try {
			return delegate.get();
		} catch (RuntimeException e) {
			if (!tolerateFailure) {
				throw new IllegalStateException(
					"Unable to load the IdP client registration; the IdP's discovery document could not be"
						+ " resolved. Login is unavailable until it can be reached.", e);
			}
			logger.warn("Failed to load client registrations. Error: {}", e.getMessage());
			return null;
		}
	}
}
