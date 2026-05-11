package com.ops.twin.controller;

import com.ops.twin.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssetHostControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void list_shouldReturnHosts() throws Exception {
        mockMvc.perform(get("/api/asset/host/list")
                .param("current", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void create_shouldAddHost() throws Exception {
        mockMvc.perform(post("/api/asset/host/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hostname\":\"test-host-01\",\"ipAddr\":\"10.0.0.100\",\"cabinetId\":\"CAB-01\",\"rackPos\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
