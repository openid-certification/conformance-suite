package net.openid.conformance.frontchannel;

import org.htmlunit.MockWebConnection;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The browser automation reads the visible text of an element to satisfy a 'wait' command, which
 * makes HtmlUnit evaluate every stylesheet rule against it. A selector the HtmlUnit version cannot
 * evaluate surfaces as a timeout in the test log, so the selectors real login pages rely on are
 * pinned here.
 */
public class HtmlUnitCssSelectorSupport_UnitTest {

	@Test
	public void readsTextWhenHasNextSiblingSelectorMeetsElementWithoutNextSibling() {
		String html = """
			<html>
			<head>
			<style>
			p.instruction:has(+ .footer) { margin-bottom: 0; }
			</style>
			</head>
			<body>
			<div id="kc-error-message"><p class="instruction">Invalid Request</p></div>
			</body>
			</html>
			""";

		HtmlUnitDriver driver = new HtmlUnitDriver(true);
		try {
			MockWebConnection connection = new MockWebConnection();
			connection.setDefaultResponse(html);
			driver.getWebClient().setWebConnection(connection);

			driver.get("http://localhost/error");

			assertEquals("Invalid Request", driver.findElement(By.id("kc-error-message")).getText());
		} finally {
			driver.quit();
		}
	}
}
