package org.ssemi.persistence;

import org.ssemi.persistence.model.Order;
import org.ssemi.persistence.model.OrderStatus;
import org.ssemi.persistence.model.Sample;
import org.ssemi.persistence.repository.JsonOrderRepository;
import org.ssemi.persistence.repository.JsonSampleRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        Path dataDir = Paths.get("data");
        Files.deleteIfExists(dataDir.resolve("samples.json"));
        Files.deleteIfExists(dataDir.resolve("orders.json"));

        JsonSampleRepository sampleRepo = new JsonSampleRepository(dataDir.resolve("samples.json"));
        JsonOrderRepository  orderRepo  = new JsonOrderRepository(dataDir.resolve("orders.json"));

        // ── Sample 시나리오 ──────────────────────────────
        section("[1] Sample 3개 저장");
        sampleRepo.save(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50));
        sampleRepo.save(new Sample("S002", "SiC 기판 B",   90, 0.92,  0));
        sampleRepo.save(new Sample("S003", "InP 기판 C",   60, 0.78, 30));
        System.out.println("저장 완료");

        section("[2] findAll → 전체 시료 목록");
        printSamples(sampleRepo.findAll());

        section("[3] findById(S002)");
        sampleRepo.findById("S002").ifPresent(s -> System.out.println(formatSample(s)));

        section("[4] S002 stock → 999 업데이트");
        Sample s2 = sampleRepo.findById("S002").orElseThrow();
        s2.setStock(999);
        sampleRepo.update(s2);
        System.out.println("업데이트 완료");

        section("[5] findById(S002) — 변경 확인");
        sampleRepo.findById("S002").ifPresent(s -> System.out.println(formatSample(s)));

        section("[6] deleteById(S003)");
        sampleRepo.deleteById("S003");
        System.out.println("삭제 완료");

        section("[7] findAll → S003 제거 확인");
        printSamples(sampleRepo.findAll());

        // ── Order 시나리오 ───────────────────────────────
        section("[8] Order 3개 저장");
        orderRepo.save(new Order("O001", "S001", "서울대 나노연구소", 10, OrderStatus.RESERVED));
        orderRepo.save(new Order("O002", "S002", "카이스트 반도체랩",  5, OrderStatus.PRODUCING));
        orderRepo.save(new Order("O003", "S001", "연세대 소재공학과",  3, OrderStatus.CONFIRMED));
        System.out.println("저장 완료");

        section("[9] findAll → 전체 주문 목록");
        printOrders(orderRepo.findAll());

        section("[10] findByStatus(RESERVED)");
        printOrders(orderRepo.findByStatus(OrderStatus.RESERVED));

        section("[11] O001 status → CONFIRMED 업데이트");
        Order o1 = orderRepo.findById("O001").orElseThrow();
        o1.setStatus(OrderStatus.CONFIRMED);
        orderRepo.update(o1);
        System.out.println("업데이트 완료");

        section("[12] findByStatus(RESERVED) — 빈 리스트 확인");
        List<Order> reserved = orderRepo.findByStatus(OrderStatus.RESERVED);
        System.out.println(reserved.isEmpty() ? "(없음)" : reserved);

        section("[13] deleteById(O002)");
        orderRepo.deleteById("O002");
        System.out.println("삭제 완료");

        section("[14] findAll → O002 제거 확인");
        printOrders(orderRepo.findAll());

        section("[15] 영속성 안내");
        System.out.println("data/ 디렉토리의 JSON 파일로 데이터가 유지됩니다.");
        System.out.println("  → " + dataDir.resolve("samples.json").toAbsolutePath());
        System.out.println("  → " + dataDir.resolve("orders.json").toAbsolutePath());
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    private static String formatSample(Sample s) {
        return String.format("  [%s] %s | avgProdTime=%d | yield=%.2f | stock=%d",
                s.getSampleId(), s.getName(), s.getAvgProductionTime(), s.getYield(), s.getStock());
    }

    private static void printSamples(List<Sample> list) {
        if (list.isEmpty()) { System.out.println("  (없음)"); return; }
        list.forEach(s -> System.out.println(formatSample(s)));
    }

    private static String formatOrder(Order o) {
        return String.format("  [%s] sampleId=%s | customer=%s | qty=%d | status=%s",
                o.getOrderId(), o.getSampleId(), o.getCustomerName(), o.getQuantity(), o.getStatus());
    }

    private static void printOrders(List<Order> list) {
        if (list.isEmpty()) { System.out.println("  (없음)"); return; }
        list.forEach(o -> System.out.println(formatOrder(o)));
    }
}
