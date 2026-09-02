package com.voyagego.traingo.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.voyagego.traingo.common.config.SecurityConfig;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

	@Autowired
	MockMvc mockMvc;

	@ParameterizedTest
	@CsvSource({
		"/admin,           admin/dashboard",
		"/admin/stations,  admin/stations",
		"/admin/routes,    admin/routes",
		"/admin/trains,    admin/trains",
		"/admin/trips,     admin/trips",
		"/admin/bookings,  admin/bookings",
		"/admin/users,     admin/users"
	})
	@WithMockUser
	void adminScreen_rendersItsOwnView(String path, String viewName) throws Exception {
		mockMvc.perform(get(path))
			.andExpect(status().isOk())
			.andExpect(view().name(viewName));
	}

	/**
	 * Renders the real templates through the admin shell. A broken fragment
	 * expression in admin/layout/base.html only fails at render time, so this
	 * is the cheapest place to catch it.
	 */
	@Test
	@WithMockUser
	void adminDashboard_rendersTheShellAroundThePage() throws Exception {
		mockMvc.perform(get("/admin"))
			.andExpect(content().string(containsString("TrainGo")))
			.andExpect(content().string(containsString("Quản lý ga")))
			.andExpect(content().string(containsString("Tổng quan")))
			.andExpect(content().string(containsString("Booking gần đây")));
	}

	/**
	 * Active state comes from the activeNav model attribute, so a page that
	 * forgets to set it silently highlights nothing.
	 */
	@Test
	@WithMockUser
	void adminStations_marksItsOwnSidebarEntryActive() throws Exception {
		mockMvc.perform(get("/admin/stations"))
			.andExpect(content().string(containsString("class=\"nav-link active\" href=\"/admin/stations\"")));
	}

	/**
	 * Same trap docs/decisions/0003-he-thong-giao-dien.md describes for the
	 * landing page: one CDN link and the demo breaks the moment the wifi does.
	 */
	@Test
	@WithMockUser
	void adminPages_loadEveryAssetFromTheApplication() throws Exception {
		mockMvc.perform(get("/admin"))
			.andExpect(content().string(containsString("/vendor/bootstrap/bootstrap.min.css")))
			.andExpect(content().string(containsString("/vendor/bootstrap-icons/bootstrap-icons.css")))
			.andExpect(content().string(containsString("/css/admin/admin.css")))
			.andExpect(content().string(not(containsString("cdn.jsdelivr.net"))))
			.andExpect(content().string(not(containsString("unpkg.com"))))
			.andExpect(content().string(not(containsString("fonts.googleapis.com"))))
			.andExpect(content().string(not(containsString("cdn.ckeditor.com"))));
	}

	/**
	 * The dashboard template was ported from a third-party project. Its
	 * author's name, licence link and social links were stripped before it
	 * entered the repository; this keeps them from creeping back in.
	 */
	@Test
	@WithMockUser
	void adminPages_carryNoTraceOfTheOriginalTemplateAuthor() throws Exception {
		mockMvc.perform(get("/admin"))
			.andExpect(content().string(not(containsString("Nazareth"))))
			.andExpect(content().string(not(containsString("MIT License"))))
			.andExpect(content().string(not(containsString("opensource.org"))))
			.andExpect(content().string(not(containsString("linkedin.com"))))
			.andExpect(content().string(not(containsString("github.com"))));
	}

	/**
	 * Admin CSS and JS live under /css/** and /js/**, which are already public,
	 * so an unstyled login page would be the only symptom of getting this
	 * wrong. Icons are the same trap one level down.
	 */
	@Test
	void adminAssets_areReachable_withoutLogin() throws Exception {
		mockMvc.perform(get("/css/admin/admin.css")).andExpect(status().isOk());
		mockMvc.perform(get("/js/admin/admin.js")).andExpect(status().isOk());
		mockMvc.perform(get("/vendor/bootstrap-icons/bootstrap-icons.css")).andExpect(status().isOk());
		mockMvc.perform(get("/design/color-palettes.html")).andExpect(status().isOk());
	}

	@Test
	void designPalette_exposesTheCompleteRailwayBlueTokenCatalog() throws Exception {
		mockMvc.perform(get("/design/color-palettes.html"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Primary Active")))
			.andExpect(content().string(containsString("--traingo-neutral-900")))
			.andExpect(content().string(containsString("--traingo-success-border")))
			.andExpect(content().string(containsString("--traingo-chart-6")))
			.andExpect(content().string(containsString("--traingo-overlay-loading")));
	}

	@Test
	void adminArea_isClosed_toAnonymousVisitors() throws Exception {
		mockMvc.perform(get("/admin"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", containsString("/login")));
	}

}
