package net.openid.conformance.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.openid.conformance.security.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Partner theming API (FEASIBILITY SPIKE).
 *
 * <p>The three GET endpoints are public (registered in
 * {@code WebSecurityResourceServerConfig}) because the login page must brand itself
 * for anonymous visitors. They are also allowlisted for private-link users, who view
 * shared results on themed pages. The write endpoints require an admin.</p>
 *
 * <p>Presets may carry pre-baked secrets (client IDs, keys, mTLS certs), so the
 * public GET strips them; only authenticated non-private-link users receive the
 * presets block.</p>
 *
 * <p>SPIKE ONLY: writes are open to any signed-in user so reviewers can try the
 * theming flow without admin rights — theme-admin.html shows a banner saying so.
 * A production version must gate writes on {@code isAdmin()} (or a future
 * partner-admin role; see docs/theming-spike/README.md).</p>
 */
@Controller
@RequestMapping(value = "/api")
public class ThemeApi {

	@Autowired
	private ThemeService themeService;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@GetMapping(value = "/theme", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get the active partner theme (brand for everyone; presets only when authenticated)")
	public ResponseEntity<Object> getTheme() {
		ThemeService.ResolvedTheme resolved = themeService.getActiveTheme();
		JsonObject body = new JsonObject();
		body.addProperty("source", resolved.source().name().toLowerCase());
		if (resolved.theme() != null) {
			JsonObject theme = resolved.theme().deepCopy();
			scrubLogoData(theme);
			boolean authenticated = (authenticationFacade.isUser() || authenticationFacade.isAdmin())
				&& !authenticationFacade.isPrivateLinkUser();
			if (!authenticated) {
				theme.remove("presets");
			}
			body.add("theme", theme);
		}
		return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(body);
	}

	@GetMapping(value = "/theme/css", produces = "text/css")
	@Operation(summary = "Get the partner accent override stylesheet (empty when no theme is active)")
	public ResponseEntity<String> getThemeCss() {
		ThemeService.ResolvedTheme resolved = themeService.getActiveTheme();
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noCache())
			.contentType(new MediaType("text", "css", StandardCharsets.UTF_8))
			.body(themeService.generateCss(resolved.theme()));
	}

	@GetMapping(value = "/theme/logo")
	@Operation(summary = "Get the partner logo binary")
	public ResponseEntity<Object> getThemeLogo() {
		ThemeService.Logo logo = themeService.getLogo(themeService.getActiveTheme());
		if (logo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		// CSP sandbox: an SVG navigated to directly is a document and could run
		// scripts; <img> consumers are unaffected by this header.
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noCache())
			.header("Content-Security-Policy", "sandbox; default-src 'none'")
			.contentType(MediaType.parseMediaType(logo.contentType()))
			.body(logo.bytes());
	}

	@PostMapping(value = "/theme", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Replace the partner theme (spike: any signed-in user; production would be admin-only)")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Theme saved"),
		@ApiResponse(responseCode = "400", description = "Theme failed validation"),
		@ApiResponse(responseCode = "403", description = "Sign-in required (spike; production would require the admin role)"),
		@ApiResponse(responseCode = "409", description = "Theme is managed by deployment configuration (fintechlabs.theme.dir)")
	})
	public ResponseEntity<Object> setTheme(@RequestBody JsonObject theme) {
		if (!mayEditTheme()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		if (themeService.isFileManaged()) {
			return errorResponse(HttpStatus.CONFLICT,
				List.of("The theme is managed by deployment configuration (fintechlabs.theme.dir); edit theme.json instead"));
		}
		List<String> errors = themeService.validate(theme);
		if (!errors.isEmpty()) {
			return errorResponse(HttpStatus.BAD_REQUEST, errors);
		}
		themeService.saveTheme(theme, authenticationFacade.getPrincipal());
		JsonObject body = new JsonObject();
		body.addProperty("result", "saved");
		return ResponseEntity.ok(body);
	}

	@DeleteMapping(value = "/theme", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Remove the partner theme, reverting to OIDF branding (spike: any signed-in user)")
	public ResponseEntity<Object> deleteTheme() {
		if (!mayEditTheme()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		if (themeService.isFileManaged()) {
			return errorResponse(HttpStatus.CONFLICT,
				List.of("The theme is managed by deployment configuration (fintechlabs.theme.dir); edit theme.json instead"));
		}
		themeService.deleteTheme();
		JsonObject body = new JsonObject();
		body.addProperty("result", "deleted");
		return ResponseEntity.ok(body);
	}

	/**
	 * SPIKE ONLY: any signed-in (non-private-link) user may edit the theme so MR
	 * reviewers can exercise the flow without admin rights. Production must check
	 * {@code authenticationFacade.isAdmin()} here instead.
	 */
	private boolean mayEditTheme() {
		return (authenticationFacade.isUser() || authenticationFacade.isAdmin())
			&& !authenticationFacade.isPrivateLinkUser();
	}

	/**
	 * Replace embedded logo binary with a stable URL: clients render
	 * {@code <img src="/api/theme/logo">} instead of inlining a possibly-large data
	 * URL into every page's theme fetch.
	 */
	private void scrubLogoData(JsonObject theme) {
		if (theme.has("brand") && theme.get("brand").isJsonObject()) {
			JsonObject brand = theme.getAsJsonObject("brand");
			if (brand.has("logo") && brand.get("logo").isJsonObject()) {
				JsonObject logo = brand.getAsJsonObject("logo");
				logo.remove("data");
				logo.remove("file");
				logo.addProperty("url", "/api/theme/logo");
			}
		}
	}

	private ResponseEntity<Object> errorResponse(HttpStatus status, List<String> errors) {
		JsonObject body = new JsonObject();
		JsonArray array = new JsonArray();
		errors.forEach(array::add);
		body.add("errors", array);
		return new ResponseEntity<>(body, status);
	}
}
