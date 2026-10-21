package az.masapay.qr;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import az.masapay.config.SecurityConfig;
import az.masapay.domain.enums.UserRole;
import az.masapay.security.AuthenticatedUser;
import az.masapay.security.JwtService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

/** Web-layer checks: role gate, content types and propagation of the restaurant scope 403. */
@WebMvcTest(AdminQrController.class)
@Import({SecurityConfig.class, JwtService.class})
class AdminQrControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private TableQrExportService exportService;

	private static Authentication as(UserRole role) {
		AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "user@masapay.az", role);
		return new UsernamePasswordAuthenticationToken(principal, null,
			List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
	}

	@Test
	void managerGetsSvg() throws Exception {
		UUID tableId = UUID.randomUUID();
		when(exportService.tableSvg(any(), eq(tableId))).thenReturn("<svg/>");

		mvc.perform(get("/api/admin/tables/{id}/qr", tableId).with(authentication(as(UserRole.RESTO_MANAGER))))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/svg+xml"));
	}

	@Test
	void superAdminGetsZipAsAttachment() throws Exception {
		UUID restaurantId = UUID.randomUUID();
		when(exportService.restaurantZip(any(), eq(restaurantId))).thenReturn(new byte[] {1, 2});

		mvc.perform(get("/api/admin/restaurants/{id}/tables/qr", restaurantId)
				.with(authentication(as(UserRole.SUPER_ADMIN))))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/zip"))
			.andExpect(header().string("Content-Disposition",
				"attachment; filename=\"table-qr-codes-" + restaurantId + ".zip\""));
	}

	@Test
	void managerOfAnotherRestaurantGets403() throws Exception {
		UUID restaurantId = UUID.randomUUID();
		when(exportService.restaurantZip(any(), eq(restaurantId)))
			.thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant"));

		mvc.perform(get("/api/admin/restaurants/{id}/tables/qr", restaurantId)
				.with(authentication(as(UserRole.RESTO_MANAGER))))
			.andExpect(status().isForbidden());
	}

	@Test
	void waiterIsForbidden() throws Exception {
		mvc.perform(get("/api/admin/tables/{id}/qr", UUID.randomUUID()).with(authentication(as(UserRole.WAITER))))
			.andExpect(status().isForbidden());
	}

	@Test
	void anonymousIsUnauthorized() throws Exception {
		mvc.perform(get("/api/admin/tables/{id}/qr", UUID.randomUUID()))
			.andExpect(status().isUnauthorized());
	}
}
