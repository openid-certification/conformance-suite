package net.openid.conformance.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.security.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@Tag(name = "UserInfoAPI", description = "Endpoint to query session info")
@RequestMapping(value = "/api")
public class UserInfoUIController {

	@Autowired
	private AuthenticationFacade authenticationFacade;

	/**
	 * Provide a JSON result that represents the currently logged in user.
	 *
	 * @return
	 */
	@GetMapping(value = "/currentuser", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get current user information")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getCurrentUserInfo() {
		Map<String, Object> map = new HashMap<>();

		Map<String, String> principal = authenticationFacade.getPrincipal();
		String displayName = authenticationFacade.getDisplayName();
		OAuth2User userInfo = authenticationFacade.getUserInfo();
		map.put("iss", principal.get("iss"));
		map.put("sub", principal.get("sub"));
		map.put("principal", principal.toString());
		map.put("displayName", displayName);
		map.put("isAdmin", authenticationFacade.isAdmin());
		map.put("userInfo", userInfo);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}
}
