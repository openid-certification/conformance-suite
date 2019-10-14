package net.openid.conformance.ui;

import java.util.HashMap;
import java.util.Map;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.openid.conformance.security.AuthenticationFacade;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
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
	@ApiOperation(value = "Get current user information")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	public ResponseEntity<Object> getCurrentUserInfo() {
		Map<String, Object> map = new HashMap<>();

		Map<String, String> principal = authenticationFacade.getPrincipal();
		String displayName = authenticationFacade.getDisplayName();
		UserInfo userInfo = authenticationFacade.getUserInfo();
		map.put("iss", principal.get("iss"));
		map.put("sub", principal.get("sub"));
		map.put("principal", principal.toString());
		map.put("displayName", displayName);
		map.put("isAdmin", authenticationFacade.isAdmin());
		map.put("userInfo", userInfo);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}
}
