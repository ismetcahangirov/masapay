package az.masapay.tip;

import az.masapay.security.AuthenticatedUser;
import az.masapay.tip.dto.MyTips;
import java.time.LocalDate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in waiter's own tips, with the table of each (#182). */
@RestController
@RequestMapping("/api/admin/me/tips")
@PreAuthorize("hasRole('WAITER')")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MyTipsController {

	private final MyTipsService service;

	public MyTipsController(MyTipsService service) {
		this.service = service;
	}

	@GetMapping
	public MyTips mine(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return service.mine(caller, from, to);
	}
}
