package hei.school;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Grade transcript of a student.
 */
@Component
public class GradeReport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> build(long studentId) {
        List<Map<String, Object>> students = jdbcTemplate.queryForList("SELECT * FROM student WHERE id = ?", studentId);
        if (students.isEmpty()) {
            return null;
        }
        Map<String, Object> student = students.get(0);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT e.title, e.deadline, e.max_points, s.id AS submission_id, s.submitted_at, s.raw_points, s.feedback "
                        + "FROM exercise e LEFT JOIN submission s ON s.exercise_id = e.id AND s.student_id = ? "
                        + "ORDER BY e.deadline",
                studentId);

        List<Map<String, Object>> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int graded = 0;
        for (Map<String, Object> row : rows) {
            Map<String, Object> line = new LinkedHashMap<>();
            LocalDateTime deadline = ((Timestamp) row.get("deadline")).toLocalDateTime();
            line.put("exercise", row.get("title"));
            line.put("deadline", deadline);

            if (row.get("submission_id") == null) {
                if (deadline.isBefore(LocalDateTime.now())) {
                    line.put("status", "NON RENDU");
                } else {
                    line.put("status", "À RENDRE");
                }
                line.put("grade", null);
            } else {
                LocalDateTime submittedAt = ((Timestamp) row.get("submitted_at")).toLocalDateTime();
                line.put("submittedAt", submittedAt);
                GradeCalculator.Result grade = row.get("raw_points") == null ? null
                        : GradeCalculator.calculate((BigDecimal) row.get("raw_points"),
                                (BigDecimal) row.get("max_points"), deadline, submittedAt);
                line.put("late", GradeCalculator.lateDays(deadline, submittedAt) > 0);
                if (row.get("raw_points") == null) {
                    line.put("status", "EN ATTENTE DE CORRECTION");
                    line.put("grade", null);
                } else {
                    line.put("status", "CORRIGÉ");
                    line.put("rawGrade", grade.rawGrade());
                    line.put("grade", grade.grade());
                    line.put("lateDays", grade.lateDays());
                    line.put("penaltyPercent", grade.penaltyPercent());
                    line.put("feedback", row.get("feedback"));
                    total = total.add(grade.grade());
                    graded++;
                }
            }
            lines.add(line);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reference", student.get("reference"));
        report.put("student", student.get("full_name"));
        report.put("email", student.get("email"));
        report.put("cohort", student.get("cohort"));
        report.put("exercises", lines);
        report.put("average", graded == 0 ? null : total.divide(new BigDecimal(graded), 2, RoundingMode.HALF_UP));
        return report;
    }
}
