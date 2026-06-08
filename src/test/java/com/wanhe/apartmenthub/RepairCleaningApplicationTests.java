package com.wanhe.apartmenthub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RepairCleaningApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void repairOrderCanMoveThroughFullFlow() throws Exception {
        String reportBody = """
                {
                  "title": "Network cable broken",
                  "description": "Network port has no signal.",
                  "repairType": "NETWORK",
                  "priority": "URGENT",
                  "reporterId": 1
                }
                """;

        String reportResponse = mockMvc.perform(post("/api/repair/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long orderId = readId(reportResponse);

        mockMvc.perform(put("/api/repair/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": %d, "assigneeId": 2}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.assigneeId").value(2));

        mockMvc.perform(put("/api/repair/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": %d, "repairFee": 80, "materialFee": 20}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_CHECK"))
                .andExpect(jsonPath("$.totalFee").value(100));

        mockMvc.perform(put("/api/repair/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": %d}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void onlyPendingRepairOrdersCanBeAssigned() throws Exception {
        String reportResponse = mockMvc.perform(post("/api/repair/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Chair needs repair",
                                  "description": "Chair leg is loose.",
                                  "repairType": "FURNITURE",
                                  "priority": "NORMAL",
                                  "reporterId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long orderId = readId(reportResponse);

        String assignBody = """
                {"orderId": %d, "assigneeId": 2}
                """.formatted(orderId);

        mockMvc.perform(put("/api/repair/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/repair/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("only PENDING orders can be assigned"));
    }

    @Test
    void cleaningPlanCanMoveFromPendingToCompleted() throws Exception {
        String createResponse = mockMvc.perform(post("/api/cleaning/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "area": "Building 2 lobby",
                                  "cleanerName": "Cleaner Zhang",
                                  "planDate": "2026-06-08",
                                  "remark": "Daily cleaning"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long planId = readId(createResponse);

        mockMvc.perform(put("/api/cleaning/plans/{id}/start", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(put("/api/cleaning/plans/{id}/complete", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/cleaning/plans"))
                .andExpect(status().isOk());
    }

    private long readId(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }
}

