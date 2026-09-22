package com.salon.crm;

import com.salon.crm.config.SegmentProperties;
import com.salon.crm.entity.Client;
import com.salon.crm.service.SegmentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SegmentServiceTest {

    private final SegmentService service = new SegmentService(new SegmentProperties());
    private final LocalDate today = LocalDate.of(2025, 6, 15);

    private Client clientCreatedDaysAgo(long days) {
        Client c = new Client();
        c.setCreatedAt(Instant.now().minus(days, ChronoUnit.DAYS));
        return c;
    }

    @Test
    void newClient_noVisits_createdRecently() {
        List<String> segments = service.segmentsFor(
                clientCreatedDaysAgo(10), null, 0, BigDecimal.ZERO, today);
        assertThat(segments).contains(SegmentService.NEW);
    }

    @Test
    void newClient_firstVisitWithin30Days() {
        Client c = clientCreatedDaysAgo(90);
        List<String> segments = service.segmentsFor(
                c, today.minusDays(10), 1, BigDecimal.valueOf(500), today);
        assertThat(segments).contains(SegmentService.NEW);
    }

    @Test
    void atRisk_lastVisit45to60Days() {
        Client c = clientCreatedDaysAgo(200);
        List<String> segments = service.segmentsFor(
                c, today.minusDays(50), 3, BigDecimal.valueOf(1000), today);
        assertThat(segments).contains(SegmentService.AT_RISK)
                .doesNotContain(SegmentService.LAPSED);
    }

    @Test
    void lapsed_lastVisitOver60Days() {
        Client c = clientCreatedDaysAgo(200);
        List<String> segments = service.segmentsFor(
                c, today.minusDays(90), 3, BigDecimal.valueOf(1000), today);
        assertThat(segments).contains(SegmentService.LAPSED)
                .doesNotContain(SegmentService.AT_RISK);
    }

    @Test
    void vip_highSpend() {
        Client c = clientCreatedDaysAgo(200);
        List<String> segments = service.segmentsFor(
                c, today.minusDays(10), 3, BigDecimal.valueOf(25000), today);
        assertThat(segments).contains(SegmentService.VIP);
    }

    @Test
    void vip_manyVisits() {
        Client c = clientCreatedDaysAgo(400);
        List<String> segments = service.segmentsFor(
                c, today.minusDays(10), 12, BigDecimal.valueOf(5000), today);
        assertThat(segments).contains(SegmentService.VIP);
    }

    @Test
    void birthdayThisMonth() {
        Client c = clientCreatedDaysAgo(200);
        c.setDateOfBirth(LocalDate.of(1990, 6, 10));
        List<String> segments = service.segmentsFor(
                c, today.minusDays(10), 1, BigDecimal.ZERO, today);
        assertThat(segments).contains(SegmentService.BIRTHDAY_THIS_MONTH);
    }

    @Test
    void noSegmentsForOldClientWithRecentVisit() {
        Client c = clientCreatedDaysAgo(400);
        c.setDateOfBirth(LocalDate.of(1990, 1, 1));
        List<String> segments = service.segmentsFor(
                c, today.minusDays(10), 3, BigDecimal.valueOf(1000), today);
        assertThat(segments).isEmpty();
    }
}
