package com.voyagego.traingo.home;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.voyagego.traingo.common.config.SecurityConfig;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void home_isPublic_andRendersIndexView() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(view().name("index"));
	}

	@Test
	void home_exposesStationsAndPopularRoutes() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(model().attributeExists("stations", "popularRoutes"));
	}

	/**
	 * Renders the real template through the shared layout. A broken fragment
	 * expression in layout/base.html only fails at render time, so this is the
	 * cheapest place to catch it.
	 */
	@Test
	void home_rendersAllFourLandingSections() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(content().string(containsString("TrainGo")))
			.andExpect(content().string(containsString("Khám phá Việt Nam bằng tàu")))
			.andExpect(content().string(containsString("Tuyến phổ biến")))
			.andExpect(content().string(containsString("Đặt vé thế nào")));
	}

	@Test
	void home_linksBootstrapServedByTheApplication() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(content().string(containsString("/vendor/bootstrap/bootstrap.min.css")))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("cdn.jsdelivr.net"))));
	}

	/**
	 * The filter chain ends with anyRequest().authenticated(). Without an
	 * explicit rule for /vendor/**, Spring Security answers this request with
	 * the login page, the browser silently discards it, and every page renders
	 * unstyled with nothing in the log to explain why.
	 */
	@Test
	void bootstrapStylesheet_isReachable_withoutLogin() throws Exception {
		mockMvc.perform(get("/vendor/bootstrap/bootstrap.min.css"))
			.andExpect(status().isOk());
	}

}
