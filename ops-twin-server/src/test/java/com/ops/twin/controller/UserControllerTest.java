package com.ops.twin.controller;

import com.ops.twin.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void list_shouldReturnUsers() throws Exception {
        mockMvc.perform(get("/api/system/user/list")
                .param("current", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void list_shouldContainAdminUser() throws Exception {
        mockMvc.perform(get("/api/system/user/list")
                .param("current", "1")
                .param("size", "10")
                .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    @Test
    void create_shouldAddUser() throws Exception {
        mockMvc.perform(post("/api/system/user/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"test123\",\"roleId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void update_shouldModifyUser() throws Exception {
        // POST /save with id triggers update logic in the controller
        mockMvc.perform(post("/api/system/user/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"username\":\"admin\",\"roleId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_shouldRemoveUser() throws Exception {
        mockMvc.perform(delete("/api/system/user/delete/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
