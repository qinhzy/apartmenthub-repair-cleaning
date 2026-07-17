package com.wanhe.apartmenthub.repository;

import com.wanhe.apartmenthub.domain.CleaningStatus;
import com.wanhe.apartmenthub.dto.CreateCleaningPlanRequest;
import com.wanhe.apartmenthub.model.CleaningPlan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CleaningPlanRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<CleaningPlan> rowMapper = (rs, rowNum) -> {
        CleaningPlan plan = new CleaningPlan();
        plan.setId(rs.getLong("id"));
        plan.setArea(rs.getString("area"));
        plan.setCleanerName(rs.getString("cleaner_name"));
        plan.setPlanDate(rs.getDate("plan_date").toLocalDate());
        Time planTime = rs.getTime("plan_time");
        plan.setPlanTime(planTime == null ? null : planTime.toLocalTime());
        plan.setStatus(CleaningStatus.valueOf(rs.getString("status")));
        plan.setRemark(rs.getString("remark"));
        plan.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        plan.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return plan;
    };

    public CleaningPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(CreateCleaningPlanRequest request) {
        String sql = """
                INSERT INTO rpt_cleaning_plan
                (area, cleaner_name, plan_date, plan_time, status, remark, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.getArea());
            ps.setString(2, request.getCleanerName());
            ps.setObject(3, request.getPlanDate());
            ps.setObject(4, request.getPlanTime());
            ps.setString(5, CleaningStatus.PENDING.name());
            ps.setString(6, request.getRemark());
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.setTimestamp(8, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public List<CleaningPlan> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM rpt_cleaning_plan ORDER BY plan_date DESC, plan_time, id DESC",
                rowMapper
        );
    }

    public List<CleaningPlan> findByPlanDate(LocalDate planDate) {
        return jdbcTemplate.query(
                """
                SELECT * FROM rpt_cleaning_plan
                WHERE plan_date = ?
                ORDER BY (plan_time IS NULL), plan_time, id
                """,
                rowMapper,
                planDate
        );
    }

    public Optional<CleaningPlan> findById(Long id) {
        List<CleaningPlan> result = jdbcTemplate.query(
                "SELECT * FROM rpt_cleaning_plan WHERE id = ?",
                rowMapper,
                id
        );
        return result.stream().findFirst();
    }

    public int updateStatus(
            Long id,
            CleaningStatus expectedStatus,
            CleaningStatus newStatus,
            LocalDateTime updatedAt
    ) {
        return jdbcTemplate.update(
                """
                UPDATE rpt_cleaning_plan
                SET status = ?, updated_at = ?
                WHERE id = ? AND status = ?
                """,
                newStatus.name(),
                Timestamp.valueOf(updatedAt),
                id,
                expectedStatus.name()
        );
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
