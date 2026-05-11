package com.ops.twin.controller;

import com.ops.twin.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void list_shouldReturnRoles() throws Exception {
        mockMvc.perform(get("/api/system/role/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void list_shouldContainAdminRole() throws Exception {
        mockMvc.perform(get("/api/system/role/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].roleCode").value("admin"));
    }
}
