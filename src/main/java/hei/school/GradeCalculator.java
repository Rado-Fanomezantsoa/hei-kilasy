package hei.school;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public final class GradeCalculator {

    private static final int MAX_PENALTY_PERCENT = 50;
    private static final BigDecimal TWENTY = new BigDecimal("20");

    private GradeCalculator() {
    }

    public static Result calculate(BigDecimal rawPoints, BigDecimal maxPoints, LocalDateTime deadline,
            LocalDateTime submittedAt) {
        int lateDays = lateDays(deadline, submittedAt);
        int penaltyPercent = Math.min(lateDays * 10, MAX_PENALTY_PERCENT);
        BigDecimal rawGrade = rawPoints.multiply(TWENTY).divide(maxPoints, 10, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
        BigDecimal grade = rawGrade.multiply(BigDecimal.valueOf(100 - penaltyPercent))
                .divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
        return new Result(rawGrade.setScale(1, RoundingMode.HALF_UP), grade, lateDays, penaltyPercent);
    }

    public static int lateDays(LocalDateTime deadline, LocalDateTime submittedAt) {
        if (!submittedAt.isAfter(deadline)) {
            return 0;
        }
        Duration delay = Duration.between(deadline, submittedAt);
        long days = delay.toDays();
        return Math.toIntExact(days + (delay.minusDays(days).isZero() ? 0 : 1));
    }

    public record Result(BigDecimal rawGrade, BigDecimal grade, int lateDays, int penaltyPercent) {
    }
}
