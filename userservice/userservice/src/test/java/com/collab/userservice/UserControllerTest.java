package com.collab.userservice;

import com.collab.userservice.dto.LoginRequest;
import com.collab.userservice.dto.RegisterRequest;
import com.collab.userservice.model.User;
import com.collab.userservice.security.JwtUtils;
import com.collab.userservice.service.UnauthorizedException;
import com.collab.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
@TestPropertySource(properties = {
        "internal.secret=test-secret",
        "services.docservice.baseUrl=http://localhost:8082"
})
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService users;

    @MockBean
    private JwtUtils jwtUtils;

    // UserController needs this now (for calling docservice on delete)
    @MockBean
    private RestTemplate restTemplate;

    // ---------- REGISTER ----------

    @Test
    void register_returns201AndUserResponse() throws Exception {
        User u = new User();
        u.setUsername("z");
        u.setEmail("z@x.com");

        when(users.register(any(RegisterRequest.class))).thenReturn(u);

        mvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"z\",\"email\":\"z@x.com\",\"password\":\"pw\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("z"))
                .andExpect(jsonPath("$.email").value("z@x.com"));
    }

    @Test
    void register_usernameExists_returns400() throws Exception {
        when(users.register(any())).thenThrow(new IllegalArgumentException("username already exists"));

        mvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"taken\",\"email\":\"a@b.com\",\"password\":\"p\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("username already exists"));
    }

    @Test
    void register_invalidInput_returns400() throws Exception {
        mvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"z\",\"email\":\"not-an-email\",\"password\":\"pw\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- LOGIN ----------

    @Test
    void login_returnsTokenAndUserInfo() throws Exception {
        User u = new User();
        u.setUsername("z");
        u.setEmail("z@x.com");

        when(users.login("z", "pw")).thenReturn(u);
        when(jwtUtils.generateToken("z")).thenReturn("TOKEN123");

        mvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"z\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN123"))
                .andExpect(jsonPath("$.username").value("z"))
                .andExpect(jsonPath("$.email").value("z@x.com"));
    }

    @Test
    void login_emptyFields_returns400() throws Exception {
        mvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- UPDATE PROFILE ----------

    @Test
    void updateMe_usesXUserHeader() throws Exception {
        User u = new User();
        u.setUsername("z");
        u.setEmail("new@x.com");

        when(users.updateEmail("z", "new@x.com")).thenReturn(u);

        mvc.perform(put("/users/me")
                        .header("X-User", "z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@x.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@x.com"));
    }

    @Test
    void updateProfile_invalidEmail_returns400() throws Exception {
        mvc.perform(put("/users/me")
                        .header("X-User", "z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- CHANGE PASSWORD ----------

    @Test
    void changePassword_returns204() throws Exception {
        doNothing().when(users).changePassword("z", "old", "new");

        mvc.perform(put("/users/me/password")
                        .header("X-User", "z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"new\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_wrongCurrent_returns401WithJsonBody() throws Exception {
        doThrow(new UnauthorizedException("current password is incorrect"))
                .when(users).changePassword(eq("z"), anyString(), anyString());

        mvc.perform(put("/users/me/password")
                        .header("X-User", "z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"new\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("current password is incorrect"));
    }

    // ---------- EXISTS ----------

    @Test
    void exists_returnsBoolean() throws Exception {
        when(users.exists("someone")).thenReturn(true);

        mvc.perform(get("/users/exists/someone"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // ---------- ME ----------

    @Test
    void me_returnsUserResponse() throws Exception {
        User u = new User();
        u.setUsername("z");
        u.setEmail("z@x.com");

        when(users.getByUsername("z")).thenReturn(u);

        mvc.perform(get("/users/me").header("X-User", "z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("z"))
                .andExpect(jsonPath("$.email").value("z@x.com"));
    }

    // ---------- DELETE ACCOUNT ----------

    @Test
    void deleteMe_returns204() throws Exception {
        doNothing().when(users).deleteByUsername("z");

        mvc.perform(delete("/users/me")
                        .header("X-User", "z"))
                .andExpect(status().isNoContent());

        verify(users).deleteByUsername("z");
    }
}
