package com.knowledge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    static Path tempUploadDir;

    @DynamicPropertySource
    static void overrideUploadDir(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> tempUploadDir.toString());
    }

    @Test
    void uploadPdf_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .param("description", "Test PDF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.originalName").value("test.pdf"))
                .andExpect(jsonPath("$.data.fileCategory").value("PDF"));
    }

    @Test
    void uploadUnsupportedFormat_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "EXE content".getBytes());

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void listFiles_returns200() throws Exception {
        mockMvc.perform(get("/api/files")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void deleteFile_thenDownload_returns404() throws Exception {
        // First upload a file
        MockMultipartFile file = new MockMultipartFile(
                "file", "todelete.png", "image/png", "PNG data".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = uploadResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        long fileId = json.path("data").path("id").asLong();
        assertThat(fileId).isGreaterThan(0);

        // Delete the file
        mockMvc.perform(delete("/api/files/" + fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Try to download — should return 404
        mockMvc.perform(get("/api/files/" + fileId + "/download"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listFiles_withFileCategory_returnsFilteredResults() throws Exception {
        // Upload a PDF file first
        MockMultipartFile file = new MockMultipartFile(
                "file", "filtered.pdf", "application/pdf", "PDF data".getBytes());
        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isOk());

        // List with PDF filter
        mockMvc.perform(get("/api/files")
                        .param("fileCategory", "PDF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }
}
