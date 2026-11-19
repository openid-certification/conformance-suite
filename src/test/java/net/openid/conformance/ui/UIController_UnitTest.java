package net.openid.conformance.ui;

import net.openid.conformance.security.LoginProviders;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UIController_UnitTest {

	/**
	 * The login providers are public, so they must carry what the login page needs and
	 * nothing else - in particular no client credentials.
	 */
	@Test
	public void loginProviders_listOnlyIdAndName() {
		LoginProviders providers = new LoginProviders(new MockEnvironment()
			.withProperty("oidc.gitlab.clientid", "gitlab-client")
			.withProperty("oidc.gitlab.secret", "gitlab-secret"));

		assertThat(new UIController(providers).getLoginProviders())
			.isEqualTo(List.of(Map.of("id", "gitlab", "name", "GitLab")));
	}

	@Test
	public void loginProviders_isEmptyWhenNoneIsConfigured() {
		assertThat(new UIController(new LoginProviders(new MockEnvironment())).getLoginProviders()).isEmpty();
	}
}
