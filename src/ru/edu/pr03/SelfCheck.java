package ru.edu.pr03;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/** Единственная точка входа: unit-проверки, data-driven режим, интерактивный режим. */
public class SelfCheck {
    private static final String DATA_FILE = "data/sample.csv";
    private static int unitTotal = 0;
    private static int unitPassed = 0;

    /** Разобранные параметры заявки. */
    private static final class Request {
        long amount;
        boolean urgent;
        RiskLevel risk;
        boolean personal;
    }

    /** Без аргументов выполняются все режимы; можно указать: unit | data | interactive. */
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "all";
        boolean all = mode.equals("all");
        boolean ok = true;
        if (all || mode.equals("unit")) {
            ok = runUnitChecks() && ok;
        }
        if (all || mode.equals("data")) {
            ok = runDataDriven() && ok;
        }
        if (!ok) {
            System.out.println("Есть FAIL – исправьте до защиты.");
            System.exit(1);
        }
        if (all || mode.equals("interactive")) {
            runInteractive();
        }
    }

    // ---------------------------------------------------------------- Режим 1
    private static boolean runUnitChecks() {
        System.out.println("=== Режим 1: unit-проверки ===");
        try {
            checkIntersections();
            checkBaseRules();
            checkBoundaries();
            checkContracts();
            checkVariantPartA();
            checkVariantPartB();
            checkExtra();
        } catch (RuntimeException e) {
            check("неожиданное исключение: " + e, false);
        }
        System.out.println("Unit: " + unitPassed + "/" + unitTotal + " passed.");
        return unitPassed == unitTotal;
    }

    private static void check(String name, boolean ok) {
        unitTotal++;
        if (ok) {
            unitPassed++;
        }
        System.out.println((ok ? "PASS " : "FAIL ") + name);
    }

    private static ApprovalRoute routeOf(long amount, boolean urgent, RiskLevel risk, boolean personal) {
        return new ApprovalRouter().route(amount, urgent, risk, personal);
    }

    /** Сообщение исключения или null, если исключения не было. */
    private static String errorOf(long amount, boolean urgent, RiskLevel risk, boolean personal) {
        try {
            routeOf(amount, urgent, risk, personal);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static void checkIntersections() {
        // route: personal приоритетнее urgent (R1 > R4)
        check("пересечение: personal > urgent",
                routeOf(10000, true, RiskLevel.LOW, true) == ApprovalRoute.SECURITY_REVIEW);
        // route: HIGH приоритетнее urgent (R2 > R4)
        check("пересечение: HIGH > urgent",
                routeOf(10000, true, RiskLevel.HIGH, false) == ApprovalRoute.DIRECTOR_REVIEW);
        // route: personal приоритетнее HIGH (R1 > R2)
        check("пересечение: personal > HIGH",
                routeOf(10000, false, RiskLevel.HIGH, true) == ApprovalRoute.SECURITY_REVIEW);
        // route: сумма приоритетнее urgent (R3 > R4)
        check("пересечение: сумма > urgent",
                routeOf(600000, true, RiskLevel.LOW, false) == ApprovalRoute.FINANCE_REVIEW);
    }

    private static void checkBaseRules() {
        // route: FAST_TRACK (для варианта 12 сумма >= 50000, иначе сработает EXPRESS)
        check("базовое: FAST_TRACK",
                routeOf(60000, true, RiskLevel.LOW, false) == ApprovalRoute.FAST_TRACK);
        // route: FINANCE_REVIEW на границе 500000
        check("базовое: FINANCE_REVIEW (граница)",
                routeOf(500000, false, RiskLevel.LOW, false) == ApprovalRoute.FINANCE_REVIEW);
        // route: STANDARD
        check("базовое: STANDARD",
                routeOf(10000, false, RiskLevel.LOW, false) == ApprovalRoute.STANDARD);
        // route: MEDIUM не активирует R2
        check("базовое: MEDIUM -> STANDARD",
                routeOf(10000, false, RiskLevel.MEDIUM, false) == ApprovalRoute.STANDARD);
        // route: SECURITY_REVIEW
        check("базовое: SECURITY_REVIEW",
                routeOf(10000, false, RiskLevel.LOW, true) == ApprovalRoute.SECURITY_REVIEW);
    }

    private static void checkBoundaries() {
        // route: 499999 срочная -> FAST_TRACK
        check("граница: 499999 -> FAST_TRACK",
                routeOf(499999, true, RiskLevel.LOW, false) == ApprovalRoute.FAST_TRACK);
        // route: 500001 -> FINANCE_REVIEW
        check("граница: 500001 -> FINANCE_REVIEW",
                routeOf(500001, false, RiskLevel.LOW, false) == ApprovalRoute.FINANCE_REVIEW);
    }

    private static void checkContracts() {
        // route: amount < 0 -> IAE с сообщением
        String negative = errorOf(-1, true, RiskLevel.LOW, false);
        check("защитное: amount < 0",
                negative != null && negative.contains("amount must be >= 0, got -1"));
        // route: risk == null -> IAE с сообщением
        String nullRisk = errorOf(10000, true, null, false);
        check("защитное: risk == null", nullRisk != null && nullRisk.contains("risk is null"));
    }

    private static void checkVariantPartA() {
        // Часть А: срочная, 10000, LOW, без персональных -> EXPRESS
        check("Часть А: 10000 срочная LOW -> EXPRESS",
                routeOf(10000, true, RiskLevel.LOW, false) == ApprovalRoute.EXPRESS);
        // Часть А: граница 49999 -> EXPRESS, 50000 -> FAST_TRACK
        check("Часть А: 49999 -> EXPRESS",
                routeOf(49999, true, RiskLevel.LOW, false) == ApprovalRoute.EXPRESS);
        check("Часть А: 50000 -> FAST_TRACK (граница не включена)",
                routeOf(50000, true, RiskLevel.LOW, false) == ApprovalRoute.FAST_TRACK);
        // Часть А: personal приоритетнее EXPRESS
        check("Часть А: personal > EXPRESS",
                routeOf(10000, true, RiskLevel.LOW, true) == ApprovalRoute.SECURITY_REVIEW);
        // Часть А: MEDIUM не даёт EXPRESS, остаётся FAST_TRACK
        check("Часть А: MEDIUM срочная -> FAST_TRACK",
                routeOf(10000, true, RiskLevel.MEDIUM, false) == ApprovalRoute.FAST_TRACK);
        // Часть А: не срочная -> STANDARD
        check("Часть А: не срочная -> STANDARD",
                routeOf(10000, false, RiskLevel.LOW, false) == ApprovalRoute.STANDARD);
    }

    private static void checkVariantPartB() {
        ApprovalRouter router = new ApprovalRouter();
        // Часть Б: addExpressEligibility – подходящая заявка
        check("Часть Б: подходит -> true",
                router.addExpressEligibility(10000, true, RiskLevel.LOW, false));
        // Часть Б: каждое из условий по отдельности даёт false
        check("Часть Б: personal -> false",
                !router.addExpressEligibility(10000, true, RiskLevel.LOW, true));
        check("Часть Б: не срочная -> false",
                !router.addExpressEligibility(10000, false, RiskLevel.LOW, false));
        check("Часть Б: сумма 50000 -> false",
                !router.addExpressEligibility(50000, true, RiskLevel.LOW, false));
        check("Часть Б: MEDIUM -> false",
                !router.addExpressEligibility(10000, true, RiskLevel.MEDIUM, false));
    }

    private static void checkExtra() {
        // route: HIGH приоритетнее суммы (R2 > R3)
        check("доп.: HIGH + 600000 -> DIRECTOR_REVIEW",
                routeOf(600000, false, RiskLevel.HIGH, false) == ApprovalRoute.DIRECTOR_REVIEW);
        // route: amount = 0 не срочная -> STANDARD
        check("доп.: amount=0 нейтральная -> STANDARD",
                routeOf(0, false, RiskLevel.LOW, false) == ApprovalRoute.STANDARD);
        // route: все три признака + HIGH -> SECURITY_REVIEW
        check("доп.: personal + HIGH + urgent -> SECURITY_REVIEW",
                routeOf(10000, true, RiskLevel.HIGH, true) == ApprovalRoute.SECURITY_REVIEW);
        // route: MEDIUM + сумма >= порога -> FINANCE_REVIEW
        check("доп.: MEDIUM + 500000 -> FINANCE_REVIEW",
                routeOf(500000, false, RiskLevel.MEDIUM, false) == ApprovalRoute.FINANCE_REVIEW);
    }

    // ---------------------------------------------------------------- Разбор строк
    private static Request parseRequest(String[] p) {
        Request r = new Request();
        r.amount = parseAmount(p[0]);
        r.urgent = parseBool(p[1], "urgent");
        r.risk = parseRisk(p[2]);
        r.personal = parseBool(p[3], "personal");
        return r;
    }

    private static long parseAmount(String text) {
        try {
            if (text.matches("-?[0-9]+")) {
                return Long.parseLong(text);
            }
        } catch (NumberFormatException e) {
            // значение слишком велико для long – сообщение формируется ниже
        }
        throw new IllegalArgumentException("Ошибка: amount не является целым числом: \"" + text + "\"");
    }

    private static boolean parseBool(String text, String name) {
        if (text.equals("true") || text.equals("false")) {
            return text.equals("true");
        }
        throw new IllegalArgumentException("Ошибка: " + name + " должен быть true или false, получено \""
                + text + "\"");
    }

    private static RiskLevel parseRisk(String text) {
        if (text.equals("null")) {
            return null;
        }
        try {
            return RiskLevel.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ошибка: risk должен быть LOW, MEDIUM или HIGH, получено \""
                    + text + "\"");
        }
    }

    // ---------------------------------------------------------------- Режим 2
    private static boolean runDataDriven() {
        System.out.println("=== Режим 2: data-driven (" + DATA_FILE + ") ===");
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(DATA_FILE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("FAIL data: не удалось прочитать " + DATA_FILE
                    + " (запускайте из корня проекта)");
            return false;
        }
        int n = 0;
        int passed = 0;
        for (String raw : lines) {
            String line = raw.replace("\uFEFF", "");
            if (line.trim().isEmpty() || line.startsWith("#") || line.startsWith("amount;urgent")) {
                continue;
            }
            n++;
            String reason = checkDataLine(line);
            System.out.println(reason == null ? "PASS data/" + n : "FAIL data/" + n + ": " + reason);
            if (reason == null) {
                passed++;
            }
        }
        System.out.println("Data-driven: " + passed + "/" + n + " passed.");
        return passed == n;
    }

    /** Возвращает null, если случай пройден, иначе причину. Некорректная строка файла = FAIL. */
    private static String checkDataLine(String line) {
        String[] p = line.split(";", -1);
        if (p.length != 5) {
            return "ожидалось 5 колонок, получено " + p.length;
        }
        Request r;
        try {
            r = parseRequest(p);
        } catch (IllegalArgumentException e) {
            return "некорректная строка файла: " + e.getMessage();
        }
        return compare(r, p[4]);
    }

    private static String compare(Request r, String expected) {
        try {
            ApprovalRoute actual = routeOf(r.amount, r.urgent, r.risk, r.personal);
            if (expected.equals("ERROR")) {
                return "ожидалось исключение, получен маршрут " + actual;
            }
            return actual.name().equals(expected) ? null : "ожидалось " + expected + ", получено " + actual;
        } catch (IllegalArgumentException e) {
            return expected.equals("ERROR") ? null : "неожиданное исключение: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------- Режим 3
    private static void runInteractive() {
        System.out.println("=== Режим 3: интерактивный (amount;urgent;risk;personal, exit или пустая строка – выход) ===");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (true) {
            System.out.print("Заявка: ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine();
            if (line.isEmpty() || line.trim().equals("exit")) {
                break;
            }
            printRoute(line);
        }
        System.out.println("Выход.");
    }

    private static void printRoute(String line) {
        String[] p = line.split(";", -1);
        if (p.length != 4) {
            System.out.println("Ошибка: ожидалось 4 значения (amount;urgent;risk;personal), получено " + p.length);
            return;
        }
        try {
            Request r = parseRequest(p);
            System.out.println("Маршрут: " + routeOf(r.amount, r.urgent, r.risk, r.personal));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}
