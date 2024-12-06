package com.mysite.restaurant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RoleTestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }


    @Test
    void testPublicEndpoint() throws Exception {
        // 누구나 접근 가능
        mockMvc.perform(get("/api/public")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("모든 사용자가 접근 가능합니다."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUserEndpointWithUserRole() throws Exception {
        // USER 권한으로 접근
        mockMvc.perform(get("/api/user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("USER 권한을 가진 사용자만 접근 가능합니다."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUserEndpointWithAdminRole() throws Exception {
        // ADMIN 권한으로 접근 (USER 전용 엔드포인트)
        mockMvc.perform(get("/api/user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // 접근 금지
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminEndpointWithAdminRole() throws Exception {
        // ADMIN 권한으로 접근
        mockMvc.perform(get("/api/admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("ADMIN 권한을 가진 사용자만 접근 가능합니다."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAdminEndpointWithUserRole() throws Exception {
        // USER 권한으로 ADMIN 전용 엔드포인트 접근
        mockMvc.perform(get("/api/admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // 접근 금지
    }
}
