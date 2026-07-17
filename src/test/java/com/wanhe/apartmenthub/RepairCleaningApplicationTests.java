package com.wanhe.apartmenthub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class RepairCleaningApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                .andExpect(status().isCreated())
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
                .andExpect(status().isCreated())
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
                                  "planTime": "09:30",
                                  "remark": "Daily cleaning"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.planTime").value("09:30:00"))
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

    @Test
    void reportValidationReturnsFieldLevelErrors() throws Exception {
        String overlongTitle = "x".repeat(101);

        mockMvc.perform(post("/api/repair/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "repairType": "NETWORK",
                                  "priority": "NORMAL",
                                  "reporterId": 1
                                }
                                """.formatted(overlongTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("request parameter validation failed"))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void pagingRejectsOutOfRangeSize() throws Exception {
        mockMvc.perform(get("/api/repair/page").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['page.size']").exists());
    }

    @Test
    void malformedJsonReturnsTheStandardApiErrorShape() throws Exception {
        mockMvc.perform(post("/api/repair/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("malformed or invalid request body"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void invalidEnumQueryParameterReturnsAFieldError() throws Exception {
        mockMvc.perform(get("/api/repair/page").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("request parameter validation failed"))
                .andExpect(jsonPath("$.errors.status").value("invalid value"));
    }

    @Test
    void databaseRejectsRepairLifecycleStatesWithMissingTimestamps() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO rpt_repair_order
                (title, repair_type, priority, status, reporter_id, assignee_id,
                 repair_fee, material_fee, total_fee, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, CURRENT_TIMESTAMP)
                """,
                "Invalid lifecycle",
                "NETWORK",
                "NORMAL",
                "PROCESSING",
                1,
                2
        ));
    }

    @Test
    void dashboardSummaryReturnsNamesCountsAndTodaysPlans() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.totalRepairs").isNumber())
                .andExpect(jsonPath("$.pendingRepairs").isNumber())
                .andExpect(jsonPath("$.recentRepairs[0].reporterName").value("报修学生"))
                .andExpect(jsonPath("$.todayCleaningPlans[0].planTime").exists());
    }

    @Test
    void repairPageSupportsTextSearchAndReturnsUserNames() throws Exception {
        mockMvc.perform(get("/api/repair/page").param("query", "公共区网络"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.records[0].title").value("公共区网络中断"))
                .andExpect(jsonPath("$.records[0].reporterName").value("报修学生"));
    }

    @Test
    void dashboardPageIsServedFromTheApplication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<title>ApartmentHub")))
                .andExpect(content().string(containsString("id=\"new-repair-button\"")));
    }

    private long readId(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }
}
