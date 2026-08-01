package com.wanhe.apartmenthub.repository;

import com.wanhe.apartmenthub.domain.Priority;
import com.wanhe.apartmenthub.domain.RepairStatus;
import com.wanhe.apartmenthub.domain.RepairType;
import com.wanhe.apartmenthub.dto.RepairReportRequest;
import com.wanhe.apartmenthub.model.RepairOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class RepairOrderRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RepairOrder> rowMapper = (rs, rowNum) -> {
        RepairOrder order = new RepairOrder();
        order.setId(rs.getLong("id"));
        order.setTitle(rs.getString("title"));
        order.setDescription(rs.getString("description"));
        order.setRepairType(RepairType.valueOf(rs.getString("repair_type")));
        order.setPriority(Priority.valueOf(rs.getString("priority")));
        order.setStatus(RepairStatus.valueOf(rs.getString("status")));
        order.setReporterId(rs.getLong("reporter_id"));
        order.setReporterName(rs.getString("reporter_name"));

        long assigneeId = rs.getLong("assignee_id");
        order.setAssigneeId(rs.wasNull() ? null : assigneeId);
        order.setAssigneeName(rs.getString("assignee_name"));

        order.setRepairFee(rs.getBigDecimal("repair_fee"));
        order.setMaterialFee(rs.getBigDecimal("material_fee"));
        order.setTotalFee(rs.getBigDecimal("total_fee"));
        order.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        order.setAssignedAt(toLocalDateTime(rs.getTimestamp("assigned_at")));
        order.setCompletedAt(toLocalDateTime(rs.getTimestamp("completed_at")));
        order.setVerifiedAt(toLocalDateTime(rs.getTimestamp("verified_at")));
        return order;
    };

    public RepairOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(RepairReportRequest request) {
        String sql = """
                INSERT INTO rpt_repair_order
                (title, description, repair_type, priority, status, reporter_id,
                 repair_fee, material_fee, total_fee, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.getTitle());
            ps.setString(2, request.getDescription());
            ps.setString(3, request.getRepairType().name());
            ps.setString(4, request.getPriority().name());
            ps.setString(5, RepairStatus.PENDING.name());
            ps.setLong(6, request.getReporterId());
            ps.setTimestamp(7, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public List<RepairOrder> findPage(
            long offset,
            int size,
            RepairStatus status,
            RepairType type,
            String queryText
    ) {
        QueryParts query = buildFilterSql(
                selectWithUserNames() + " WHERE 1 = 1",
                status,
                type,
                queryText
        );
        query.sql.append(" ORDER BY repair_order.id DESC LIMIT ? OFFSET ?");
        query.params.add(size);
        query.params.add(offset);
        return jdbcTemplate.query(query.sql.toString(), rowMapper, query.params.toArray());
    }

    public long count(RepairStatus status, RepairType type, String queryText) {
        QueryParts query = buildFilterSql(
                "SELECT COUNT(*) FROM rpt_repair_order repair_order WHERE 1 = 1",
                status,
                type,
                queryText
        );
        return jdbcTemplate.queryForObject(query.sql.toString(), Long.class, query.params.toArray());
    }

    public List<StatusCount> countByStatus() {
        return jdbcTemplate.query(
                "SELECT status, COUNT(*) AS total FROM rpt_repair_order GROUP BY status",
                (rs, rowNum) -> new StatusCount(
                        RepairStatus.valueOf(rs.getString("status")),
                        rs.getLong("total")
                )
        );
    }

    public long countOpenUrgent() {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM rpt_repair_order
                WHERE priority = ? AND status <> ?
                """,
                Long.class,
                Priority.URGENT.name(),
                RepairStatus.COMPLETED.name()
        );
        return count == null ? 0 : count;
    }

    public Optional<RepairOrder> findById(Long id) {
        List<RepairOrder> result = jdbcTemplate.query(
                selectWithUserNames() + " WHERE repair_order.id = ?",
                rowMapper,
                id
        );
        return result.stream().findFirst();
    }

    public boolean existsUser(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE id = ?",
                Integer.class,
                userId
        );
        return count != null && count > 0;
    }

    public int assign(
            Long id,
            Long assigneeId,
            LocalDateTime assignedAt,
            RepairStatus expectedStatus
    ) {
        return jdbcTemplate.update(
                """
                UPDATE rpt_repair_order
                SET assignee_id = ?, status = ?, assigned_at = ?
                WHERE id = ? AND status = ?
                """,
                assigneeId,
                RepairStatus.PROCESSING.name(),
                Timestamp.valueOf(assignedAt),
                id,
                expectedStatus.name()
        );
    }

    public int complete(
            Long id,
            BigDecimal repairFee,
            BigDecimal materialFee,
            BigDecimal totalFee,
            LocalDateTime completedAt,
            RepairStatus expectedStatus
    ) {
        return jdbcTemplate.update(
                """
                UPDATE rpt_repair_order
                SET repair_fee = ?, material_fee = ?, total_fee = ?, status = ?, completed_at = ?
                WHERE id = ? AND status = ?
                """,
                repairFee,
                materialFee,
                totalFee,
                RepairStatus.WAITING_CHECK.name(),
                Timestamp.valueOf(completedAt),
                id,
                expectedStatus.name()
        );
    }

    public int verify(Long id, LocalDateTime verifiedAt, RepairStatus expectedStatus) {
        return jdbcTemplate.update(
                """
                UPDATE rpt_repair_order
                SET status = ?, verified_at = ?
                WHERE id = ? AND status = ?
                """,
                RepairStatus.COMPLETED.name(),
                Timestamp.valueOf(verifiedAt),
                id,
                expectedStatus.name()
        );
    }

    private QueryParts buildFilterSql(
            String baseSql,
            RepairStatus status,
            RepairType type,
            String queryText
    ) {
        QueryParts query = new QueryParts(baseSql);
        if (status != null) {
            query.sql.append(" AND repair_order.status = ?");
            query.params.add(status.name());
        }
        if (type != null) {
            query.sql.append(" AND repair_order.repair_type = ?");
            query.params.add(type.name());
        }
        if (queryText != null && !queryText.isBlank()) {
            query.sql.append(
                    " AND (LOWER(repair_order.title) LIKE ? ESCAPE '!'"
                            + " OR LOWER(repair_order.description) LIKE ? ESCAPE '!')"
            );
            String pattern = "%" + escapeLikeLiteral(queryText.toLowerCase(Locale.ROOT)) + "%";
            query.params.add(pattern);
            query.params.add(pattern);
        }
        return query;
    }

    private String escapeLikeLiteral(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private String selectWithUserNames() {
        return """
                SELECT repair_order.*,
                       reporter.real_name AS reporter_name,
                       assignee.real_name AS assignee_name
                FROM rpt_repair_order repair_order
                JOIN sys_user reporter ON reporter.id = repair_order.reporter_id
                LEFT JOIN sys_user assignee ON assignee.id = repair_order.assignee_id
                """;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static class QueryParts {
        private final StringBuilder sql;
        private final List<Object> params = new ArrayList<>();

        private QueryParts(String sql) {
            this.sql = new StringBuilder(sql);
        }
    }

    public record StatusCount(RepairStatus status, long total) {
    }
}
