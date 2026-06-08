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

        long assigneeId = rs.getLong("assignee_id");
        order.setAssigneeId(rs.wasNull() ? null : assigneeId);

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

    public List<RepairOrder> findPage(int offset, int size, RepairStatus status, RepairType type) {
        QueryParts query = buildFilterSql(
                "SELECT * FROM rpt_repair_order WHERE 1 = 1",
                status,
                type
        );
        query.sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        query.params.add(size);
        query.params.add(offset);
        return jdbcTemplate.query(query.sql.toString(), rowMapper, query.params.toArray());
    }

    public long count(RepairStatus status, RepairType type) {
        QueryParts query = buildFilterSql(
                "SELECT COUNT(*) FROM rpt_repair_order WHERE 1 = 1",
                status,
                type
        );
        return jdbcTemplate.queryForObject(query.sql.toString(), Long.class, query.params.toArray());
    }

    public Optional<RepairOrder> findById(Long id) {
        List<RepairOrder> result = jdbcTemplate.query(
                "SELECT * FROM rpt_repair_order WHERE id = ?",
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

    public void assign(Long id, Long assigneeId, LocalDateTime assignedAt) {
        jdbcTemplate.update(
                """
                UPDATE rpt_repair_order
                SET assignee_id = ?, status = ?, assigned_at = ?
                WHERE id = ?
                """,
                assigneeId,
                RepairStatus.PROCESSING.name(),
                Timestamp.valueOf(assignedAt),
                id
        );
    }

    public void complete(Long id, BigDecimal repairFee, BigDecimal materialFee, BigDecimal totalFee, LocalDateTime completedAt) {
        jdbcTemplate.update(
                """
                UPDATE rpt_repair_order
                SET repair_fee = ?, material_fee = ?, total_fee = ?, status = ?, completed_at = ?
                WHERE id = ?
                """,
                repairFee,
                materialFee,
                totalFee,
                RepairStatus.WAITING_CHECK.name(),
                Timestamp.valueOf(completedAt),
                id
        );
    }

    public void verify(Long id, LocalDateTime verifiedAt) {
        jdbcTemplate.update(
                """
                UPDATE rpt_repair_order
                SET status = ?, verified_at = ?
                WHERE id = ?
                """,
                RepairStatus.COMPLETED.name(),
                Timestamp.valueOf(verifiedAt),
                id
        );
    }

    private QueryParts buildFilterSql(String baseSql, RepairStatus status, RepairType type) {
        QueryParts query = new QueryParts(baseSql);
        if (status != null) {
            query.sql.append(" AND status = ?");
            query.params.add(status.name());
        }
        if (type != null) {
            query.sql.append(" AND repair_type = ?");
            query.params.add(type.name());
        }
        return query;
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
}

