package net.openid.conformance.frontchannel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

/**
 * The scripted browser's settings: which engine runs a test configuration's "browser" block
 * ({@code browser.engine}) and the Playwright engine's own settings ({@code browser.playwright.*},
 * see {@link PlaywrightBrowserRunner.Settings}).
 *
 * <p>Everything that needs one of these settings takes it from this bean, so the code that
 * writes a trace and the code that serves it cannot be configured apart. The values come from
 * the Spring environment, which a {@code -D} system property, an environment variable and
 * {@code application.properties} all feed.
 */
@Component
public class BrowserSettings {

	public static final String ENGINE_SELENIUM = "selenium";
	public static final String ENGINE_PLAYWRIGHT = "playwright";

	private final String engine;
	private final PlaywrightBrowserRunner.Settings playwright;

	@Autowired
	public BrowserSettings(Environment environment) {
		this(environment::getProperty);
	}

	/**
	 * @param lookup the value of a setting by its full name (e.g. {@code browser.playwright.type}),
	 *               or null if it is not set
	 * @throws IllegalStateException if {@code browser.engine} names an engine that does not exist
	 */
	public BrowserSettings(Function<String, String> lookup) {
		String configuredEngine = lookup.apply("browser.engine");
		this.engine = configuredEngine == null ? ENGINE_SELENIUM : configuredEngine.toLowerCase(Locale.ROOT);
		if (!engine.equals(ENGINE_SELENIUM) && !engine.equals(ENGINE_PLAYWRIGHT)) {
			throw new IllegalStateException("Unsupported browser.engine '" + engine + "', expected "
				+ ENGINE_SELENIUM + " or " + ENGINE_PLAYWRIGHT);
		}
		this.playwright = PlaywrightBrowserRunner.Settings.from(lookup);
	}

	/**
	 * @return the scripted browser engine in use, {@link #ENGINE_SELENIUM} or {@link #ENGINE_PLAYWRIGHT}
	 */
	public String engine() {
		return engine;
	}

	public boolean isPlaywright() {
		return engine.equals(ENGINE_PLAYWRIGHT);
	}

	/**
	 * @return the directory the Playwright engine writes its trace archives to, empty if none is configured
	 */
	public String tracesDir() {
		return playwright.tracesDir();
	}

	PlaywrightBrowserRunner.Settings playwright() {
		return playwright;
	}

	/**
	 * Where the traces of a test are kept. The one definition of the layout, shared by the
	 * runner that writes a trace and the service that serves it.
	 *
	 * @param tracesDir the configured traces directory
	 * @param testId    the test instance id
	 * @return the directory holding that test's traces
	 */
	public static Path traceDirectory(String tracesDir, String testId) {
		return Path.of(tracesDir).resolve(testId);
	}

	/**
	 * @param tracesDir the configured traces directory
	 * @param testId    the test instance id
	 * @param visit     the number of the scripted browser visit within the test, from 1
	 * @return the trace archive of that visit
	 */
	public static Path traceFile(String tracesDir, String testId, int visit) {
		return traceDirectory(tracesDir, testId).resolve(visit + ".zip");
	}
}
