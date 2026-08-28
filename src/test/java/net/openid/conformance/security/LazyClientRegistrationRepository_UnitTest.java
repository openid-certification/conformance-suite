package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolving the IdP's client registration fetches its discovery document. These
 * tests pin the properties that keep a brief IdP outage from stopping the suite
 * from starting: nothing is resolved until a registration is asked for, and a
 * failed attempt is retried rather than remembered.
 */
public class LazyClientRegistrationRepository_UnitTest {

	private static final String REGISTRATION_ID = "idp";

	private static ClientRegistrationRepository repositoryWithIdp() {
		ClientRegistration registration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
			.clientId("a-client")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/idp")
			.authorizationUri("https://idp.example.invalid/authorize")
			.tokenUri("https://idp.example.invalid/token")
			.build();
		return new InMemoryClientRegistrationRepository(registration);
	}

	@Test
	public void nothing_is_resolved_until_a_registration_is_requested() {
		AtomicInteger resolutions = new AtomicInteger();

		var repository = new LazyClientRegistrationRepository(() -> {
			resolutions.incrementAndGet();
			return repositoryWithIdp();
		}, false);

		Assertions.assertEquals(0, resolutions.get(),
			"constructing the repository must not reach the IdP - that would tie startup to it");

		Assertions.assertNotNull(repository.findByRegistrationId(REGISTRATION_ID));
		Assertions.assertEquals(1, resolutions.get());
	}

	@Test
	public void a_successful_resolution_is_reused() {
		AtomicInteger resolutions = new AtomicInteger();

		var repository = new LazyClientRegistrationRepository(() -> {
			resolutions.incrementAndGet();
			return repositoryWithIdp();
		}, false);

		repository.findByRegistrationId(REGISTRATION_ID);
		repository.findByRegistrationId(REGISTRATION_ID);
		repository.findByRegistrationId(REGISTRATION_ID);

		Assertions.assertEquals(1, resolutions.get(), "the discovery document must not be re-fetched per lookup");
	}

	@Test
	public void an_unknown_registration_id_yields_null() {
		var repository = new LazyClientRegistrationRepository(
			LazyClientRegistrationRepository_UnitTest::repositoryWithIdp, false);

		Assertions.assertNull(repository.findByRegistrationId("no-such-registration"));
	}

	/**
	 * The whole point of deferring resolution is that an IdP outage is survivable.
	 * Remembering the failure would leave the process unable to log anyone in until
	 * it was restarted, which is barely better than failing to start.
	 */
	@Test
	public void a_failed_resolution_is_retried_on_the_next_lookup() {
		AtomicInteger attempts = new AtomicInteger();

		var repository = new LazyClientRegistrationRepository(() -> {
			if (attempts.incrementAndGet() == 1) {
				throw new IllegalArgumentException("Unable to resolve Configuration with the provided Issuer");
			}
			return repositoryWithIdp();
		}, false);

		Assertions.assertThrows(IllegalStateException.class,
			() -> repository.findByRegistrationId(REGISTRATION_ID));

		Assertions.assertNotNull(repository.findByRegistrationId(REGISTRATION_ID),
			"the retry must succeed once the IdP is back");
		Assertions.assertEquals(2, attempts.get());
	}

	@Test
	public void a_failure_is_reported_with_an_actionable_message_when_not_tolerated() {
		var repository = new LazyClientRegistrationRepository(() -> {
			throw new IllegalArgumentException("Unable to resolve Configuration with the provided Issuer");
		}, false);

		IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
			() -> repository.findByRegistrationId(REGISTRATION_ID));

		Assertions.assertTrue(e.getMessage().contains("IdP client registration"), e.getMessage());
		Assertions.assertNotNull(e.getCause(), "the underlying resolution failure must not be swallowed");
	}

	/** Dev mode degrades to "no registrations" rather than propagating. */
	@Test
	public void a_tolerated_failure_yields_no_registration() {
		var repository = new LazyClientRegistrationRepository(() -> {
			throw new IllegalArgumentException("IdP unreachable");
		}, true);

		Assertions.assertNull(Assertions.assertDoesNotThrow(
			() -> repository.findByRegistrationId(REGISTRATION_ID)));
	}

	/**
	 * Spring Security's OAuth2LoginConfigurer#getLoginLinks type-checks the
	 * repository for Iterable&lt;ClientRegistration&gt; and iterates it while the
	 * filter chain is built — at startup. Implementing Iterable here would resolve
	 * discovery eagerly and silently undo everything above.
	 */
	@Test
	public void the_repository_is_not_iterable() {
		Assertions.assertFalse(Iterable.class.isAssignableFrom(LazyClientRegistrationRepository.class),
			"an Iterable repository is iterated at startup, which would defeat the laziness");
	}
}
