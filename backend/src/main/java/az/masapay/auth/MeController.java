package az.masapay.auth;

import az.masapay.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Returns the currently authenticated user, resolved from the access token. */
@RestController
public class MeController {

	@GetMapping("/api/me")
	public AuthenticatedUser me(@AuthenticationPrincipal AuthenticatedUser principal) {
		return principal;
	}
}
