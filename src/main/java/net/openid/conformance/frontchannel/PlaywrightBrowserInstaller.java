package net.openid.conformance.frontchannel;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * When the scripted browser engine is Playwright ({@code -Dbrowser.engine=playwright}), downloads
 * the configured browser during server startup, so the download happens once before any test runs
 * and the server only becomes ready once it is done, instead of inside the first test's runner.
 */
@Component
public class PlaywrightBrowserInstaller {

	private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserInstaller.class);

	@PostConstruct
	public void installBrowser() {
		String engine = System.getProperty("browser.engine", BrowserControl.ENGINE_SELENIUM).toLowerCase(Locale.ROOT);
		if (!engine.equals(BrowserControl.ENGINE_PLAYWRIGHT)) {
			return;
		}
		String browserType = PlaywrightBrowserRunner.Settings.fromSystemProperties().browserType();
		try {
			PlaywrightBrowserRunner.ensureBrowserInstalled(browserType);
		} catch (RuntimeException e) {
			// don't take the whole server down: the runner retries the install when it is first needed
			logger.error("Failed to install Playwright's " + browserType + " at startup; scripted browser runs will retry", e);
		}
	}
}
