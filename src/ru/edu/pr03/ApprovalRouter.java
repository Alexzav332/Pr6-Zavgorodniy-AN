package ru.edu.pr03;

/** Маршрутизация служебных заявок. Без состояния и без ввода-вывода. */
public class ApprovalRouter {
    public static final long FINANCE_LIMIT = 500_000L;
    /** Вариант 12: верхняя граница суммы (не включая) для маршрута EXPRESS. */
    public static final long EXPRESS_LIMIT = 50_000L;

    /** Правила применяются строго по приоритету: R1, R2, R3, EXPRESS, R4, R5. */
    public ApprovalRoute route(long amount, boolean urgent, RiskLevel risk, boolean containsPersonalData) {
        checkArguments(amount, risk);
        if (requiresSecurityReview(containsPersonalData)) { return ApprovalRoute.SECURITY_REVIEW; }
        if (requiresDirectorReview(risk)) { return ApprovalRoute.DIRECTOR_REVIEW; }
        if (requiresFinanceReview(amount)) { return ApprovalRoute.FINANCE_REVIEW; }
        if (addExpressEligibility(amount, urgent, risk, containsPersonalData)) { return ApprovalRoute.EXPRESS; }
        if (requiresFastTrack(amount, urgent)) { return ApprovalRoute.FAST_TRACK; }
        return ApprovalRoute.STANDARD;
    }

    /** Вариант 12, часть Б: подходит ли заявка для экспресс-маршрута. */
    public boolean addExpressEligibility(long amount, boolean urgent, RiskLevel risk,
                                         boolean containsPersonalData) {
        return urgent && amount < EXPRESS_LIMIT && risk == RiskLevel.LOW && !containsPersonalData;
    }

    private void checkArguments(long amount, RiskLevel risk) {
        if (amount < 0) {
            throw new IllegalArgumentException("Ошибка: amount must be >= 0, got " + amount);
        }
        if (risk == null) {
            throw new IllegalArgumentException("Ошибка: risk is null");
        }
    }

    private boolean requiresSecurityReview(boolean containsPersonalData) {
        return containsPersonalData;
    }

    private boolean requiresDirectorReview(RiskLevel risk) {
        return risk == RiskLevel.HIGH;
    }

    private boolean requiresFinanceReview(long amount) {
        return amount >= FINANCE_LIMIT;
    }

    private boolean requiresFastTrack(long amount, boolean urgent) {
        return urgent && amount < FINANCE_LIMIT;
    }
}
