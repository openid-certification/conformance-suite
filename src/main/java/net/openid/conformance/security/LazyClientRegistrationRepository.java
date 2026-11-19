package net.openid.conformance.security;

import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Resolves each login client registration on first use rather than at startup, and
 * independently of the others.
 * <p>
 * Building a registration from an issuer location fetches that OpenID Provider's
 * discovery document. Doing that while the bean is created ties application startup
 * to every login provider being reachable: a momentary outage of any one of them —
 * a single 503 is enough — stops the conformance suite from starting at all,
 * including for the many tests and API calls that never touch login. Resolving all
 * of them together on first use would still let one unreachable provider block
 * logins through every other, so each registration is resolved and cached on its
 * own.
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

	private final Map<String, Supplier<ClientRegistration>> registrations;

	private final boolean tolerateFailure;

	/**
	 * @param registrations   for each registration id, resolves that registration;
	 *                        may perform network I/O and may throw
	 * @param tolerateFailure when true, a resolution failure logs and yields no
	 *                        registration rather than propagating
	 */
	public LazyClientRegistrationRepository(Map<String, Supplier<ClientRegistration>> registrations,
		boolean tolerateFailure) {
		// Guava's memoizing supplier is thread safe and does not memoize a thrown
		// exception, so only a successful resolution is cached.
		this.registrations = registrations.entrySet().stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Suppliers.memoize(entry.getValue()::get)));
		this.tolerateFailure = tolerateFailure;
	}

	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		Supplier<ClientRegistration> registration = registrations.get(registrationId);
		if (registration == null) {
			return null;
		}
		try {
			return registration.get();
		} catch (RuntimeException e) {
			if (!tolerateFailure) {
				throw new IllegalStateException(
					"Unable to load the login client registration '" + registrationId + "'; its OpenID Provider's"
						+ " discovery document could not be resolved. Login with it is unavailable until it can be"
						+ " reached.", e);
			}
			logger.warn("Failed to load client registration '{}'. Error: {}", registrationId, e.getMessage());
			return null;
		}
	}
}
