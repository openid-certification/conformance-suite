package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

public class LoginProviders_UnitTest {

	private static List<String> configuredIds(MockEnvironment environment) {
		return new LoginProviders(environment).configured().stream().map(LoginProviders.Provider::id).toList();
	}

	@Test
	public void no_provider_is_offered_without_credentials() {
		Assertions.assertEquals(List.of(), configuredIds(new MockEnvironment()));
	}

	@Test
	public void a_provider_is_offered_once_its_client_id_and_secret_are_set() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("oidc.gitlab.clientid", "gitlab-client")
			.withProperty("oidc.gitlab.secret", "gitlab-secret");

		Assertions.assertEquals(List.of("gitlab"), configuredIds(environment));
	}

	/**
	 * The Dockerfile passes -Doidc.google.clientid=${OIDC_GOOGLE_CLIENTID}, which is
	 * an empty string when the variable is unset; that must read as "not configured".
	 */
	@Test
	public void a_blank_or_missing_credential_leaves_the_provider_out() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("oidc.google.clientid", "")
			.withProperty("oidc.google.secret", "")
			.withProperty("oidc.gitlab.clientid", "gitlab-client");

		Assertions.assertEquals(List.of(), configuredIds(environment));
	}

	@Test
	public void providers_keep_the_login_page_order() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("oidc.gitlab.clientid", "gitlab-client")
			.withProperty("oidc.gitlab.secret", "gitlab-secret")
			.withProperty("oidc.google.clientid", "google-client")
			.withProperty("oidc.google.secret", "google-secret");

		Assertions.assertEquals(List.of("google", "gitlab"), configuredIds(environment));
	}

	@Test
	public void the_issuer_defaults_per_provider_and_can_be_overridden() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("oidc.google.clientid", "google-client")
			.withProperty("oidc.google.secret", "google-secret")
			.withProperty("oidc.gitlab.clientid", "gitlab-client")
			.withProperty("oidc.gitlab.secret", "gitlab-secret")
			.withProperty("oidc.gitlab.iss", "https://gitlab.example.com");

		List<LoginProviders.Provider> providers = new LoginProviders(environment).configured();
		Assertions.assertEquals("https://accounts.google.com", providers.get(0).issuer());
		Assertions.assertEquals("https://gitlab.example.com", providers.get(1).issuer());
	}
}
