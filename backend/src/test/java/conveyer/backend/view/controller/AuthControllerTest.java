package conveyer.backend.view.controller;


import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;
import conveyer.backend.DTO.SignInRequestDTO;
import conveyer.backend.DTO.SignInResponseDTO;
import conveyer.backend.DTO.SignUpDTO;
import conveyer.backend.business.service.AuthService;
import conveyer.backend.configuration.CustomOAuth2UserService;
import conveyer.backend.configuration.security.JwtAuthenticationFilter;
import conveyer.backend.configuration.filter.RateLimitingFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthService authService;

  // Beans required by SecurityConfig to load the application context
  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @MockitoBean
  private RateLimitingFilter rateLimitingFilter;

  @MockitoBean
  private CustomOAuth2UserService customOAuth2UserService;

  // --- /signup ---

  @Test
  void signUp_withNullEmail_returns400() throws Exception {
    var dto = new SignUpDTO("user", null, "pass");

    mockMvc.perform(post("/api/auth/signup")
            .header("X-API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void signUp_withEmptyFields_returns400() throws Exception {
    var dto = new SignUpDTO("", "", "");

    mockMvc.perform(post("/api/auth/signup")
            .header("X-API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void signUp_withValidParams_returns201() throws Exception {
    var dto = new SignUpDTO("user", "a@b.com", "pass");
    when(authService.createLocalAccount(any())).thenReturn(true);

    mockMvc.perform(post("/api/auth/signup")
            .header("X-API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated());
  }

  // --- /signin ---

  @Test
  void signIn_withNullEmail_returns400() throws Exception {
    var dto = new SignInRequestDTO(null, "pass");

    mockMvc.perform(post("/api/auth/signin")
            .header("X-API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void signIn_withNullPassword_returns400() throws Exception {
    var dto = new SignInRequestDTO("a@b.com", null);

    mockMvc.perform(post("/api/auth/signin")
            .header("X-API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void signIn_withEmptyFields_returns400() throws Exception {
    var dto = new SignInRequestDTO("", "");

    mockMvc.perform(post("/api/auth/signin")
            .header("X-API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void signIn_withInvalidCredentials_returns401() throws Exception {
    var dto = new SignInRequestDTO("a@b.com", "wrong");
    when(authService.signIn(any())).thenReturn(null);

    mockMvc.perform(post("/api/auth/signin")
            .header("X-API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void signIn_withValidParams_returns200() throws Exception {
    var dto = new SignInRequestDTO("a@b.com", "pass");
    when(authService.signIn(any())).thenReturn(
        new SignInResponseDTO("access-token", "refresh-token"));

    mockMvc.perform(post("/api/auth/signin")
            .header("X-API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("accessToken"))
        .andExpect(cookie().exists("refreshToken"))
        .andExpect(cookie().httpOnly("accessToken", true))
        .andExpect(cookie().httpOnly("refreshToken", true))
        .andExpect(cookie().secure("accessToken", true))
        .andExpect(cookie().path("accessToken", "/"))
        .andExpect(cookie().path("refreshToken", "/api/auth/refresh"))
        .andExpect(content().string(""));
  }

  // --- /status ---

  @Test
  void status_withMissingCookie_returns401() throws Exception {
    mockMvc.perform(get("/api/auth/status")
            .header("X-API-Version", "1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void status_withInvalidToken_returns401() throws Exception {
    when(authService.checkStatus("bad-token")).thenReturn(null);

    mockMvc.perform(get("/api/auth/status")
            .header("X-API-Version", "1")
            .cookie(new Cookie("accessToken", "bad-token")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void status_withValidToken_returns200() throws Exception {
    when(authService.checkStatus("good-token")).thenReturn("a@b.com");

    mockMvc.perform(get("/api/auth/status")
            .header("X-API-Version", "1")
            .cookie(new Cookie("accessToken", "good-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("a@b.com"))
        .andExpect(jsonPath("$.authenticated").value(true));
  }

  // --- /logout ---

  @Test
  void logout_clearsCookiesAndReturns200() throws Exception {
    mockMvc.perform(post("/api/auth/logout")
            .header("X-API-Version", "1")
            .cookie(new Cookie("accessToken", "some-token")))
        .andExpect(status().isOk())
        .andExpect(cookie().value("accessToken", ""))
        .andExpect(cookie().value("refreshToken", ""))
        .andExpect(cookie().maxAge("accessToken", 0))
        .andExpect(cookie().maxAge("refreshToken", 0))
        .andExpect(cookie().path("accessToken", "/"))
        .andExpect(cookie().path("refreshToken", "/api/auth/refresh"))
        .andExpect(content().string(""));
  }

  @Test
  void logout_withoutCookie_returns200() throws Exception {
    mockMvc.perform(post("/api/auth/logout")
            .header("X-API-Version", "1"))
        .andExpect(status().isOk())
        .andExpect(cookie().value("accessToken", ""))
        .andExpect(cookie().value("refreshToken", ""))
        .andExpect(cookie().maxAge("accessToken", 0))
        .andExpect(cookie().maxAge("refreshToken", 0));
  }

  // --- /refresh ---

  @Test
  void refresh_withMissingCookie_returns400() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")
            .header("X-API-Version", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void refresh_withEmptyCookie_returns400() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")
            .header("X-API-Version", "1")
            .cookie(new Cookie("refreshToken", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void refresh_withInvalidToken_returns401() throws Exception {
    when(authService.refreshAccessToken("invalid-token")).thenReturn(null);

    mockMvc.perform(post("/api/auth/refresh")
            .header("X-API-Version", "1")
            .cookie(new Cookie("refreshToken", "invalid-token")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void refresh_withValidToken_returns200() throws Exception {
    when(authService.refreshAccessToken("valid-refresh-token")).thenReturn(
        new SignInResponseDTO("new-access-token", "new-refresh-token"));

    mockMvc.perform(post("/api/auth/refresh")
            .header("X-API-Version", "1")
            .cookie(new Cookie("refreshToken", "valid-refresh-token")))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("accessToken"))
        .andExpect(cookie().exists("refreshToken"))
        .andExpect(cookie().httpOnly("accessToken", true))
        .andExpect(cookie().httpOnly("refreshToken", true))
        .andExpect(cookie().path("accessToken", "/"))
        .andExpect(cookie().path("refreshToken", "/api/auth/refresh"))
        .andExpect(content().string(""));
  }

}
