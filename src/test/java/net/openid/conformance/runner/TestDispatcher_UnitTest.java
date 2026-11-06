package net.openid.conformance.runner;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDispatcher_UnitTest {

	private TestRunnerSupport support = mock(TestRunnerSupport.class);

	@Test
	public void rootWellKnownRoutesToRootWellKnownAlias() {
		when(support.hasAlias("root-well-known")).thenReturn(true);
		when(support.getTestIdForAlias("root-well-known")).thenReturn("the-id");

		TestDispatcher.RouteTarget target = TestDispatcher.resolveRoute("/.well-known/poppy.json", "poppy.json", support);

		assertNull(target.error());
		assertEquals("the-id", target.testId());
		assertEquals("poppy.json", target.restOfPath());
	}

	@Test
	public void rootWellKnownWithoutAliasIsNotFound() {
		when(support.hasAlias("root-well-known")).thenReturn(false);

		TestDispatcher.RouteTarget target = TestDispatcher.resolveRoute("/.well-known/poppy.json", "poppy.json", support);

		assertNotNull(target.error());
		assertEquals(HttpStatus.NOT_FOUND, target.error().getStatusCode());
		assertEquals(Map.of("error", "No test found with alias 'root-well-known'"), target.error().getBody());
	}

	@Test
	public void wellKnownPathInsertionFormStillResolvesAlias() {
		when(support.hasAlias("x")).thenReturn(true);
		when(support.getTestIdForAlias("x")).thenReturn("the-id");

		TestDispatcher.RouteTarget target = TestDispatcher.resolveRoute(
			"/.well-known/oauth-authorization-server/test/a/x", "oauth-authorization-server/test/a/x", support);

		assertNull(target.error());
		assertEquals("the-id", target.testId());
		assertEquals("", target.restOfPath());
	}

	@Test
	public void aliasedTestPathResolvesAlias() {
		when(support.hasAlias("x")).thenReturn(true);
		when(support.getTestIdForAlias("x")).thenReturn("the-id");

		TestDispatcher.RouteTarget target = TestDispatcher.resolveRoute("/test/a/x/callback", "a/x/callback", support);

		assertNull(target.error());
		assertEquals("the-id", target.testId());
		assertEquals("callback", target.restOfPath());
	}

	@Test
	public void unknownAliasIsNotFound() {
		when(support.hasAlias("x")).thenReturn(false);

		TestDispatcher.RouteTarget target = TestDispatcher.resolveRoute("/test/a/x/callback", "a/x/callback", support);

		assertEquals(HttpStatus.NOT_FOUND, target.error().getStatusCode());
		assertEquals(Map.of("error", "No test found with alias 'x'"), target.error().getBody());
	}

	@Test
	public void testIdPathResolvesId() {
		TestDispatcher.RouteTarget target = TestDispatcher.resolveRoute("/test/abc123/token", "abc123/token", support);

		assertNull(target.error());
		assertEquals("abc123", target.testId());
		assertEquals("token", target.restOfPath());
	}
}
