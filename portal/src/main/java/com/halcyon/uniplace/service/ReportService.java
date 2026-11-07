package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.Offer;
import com.halcyon.uniplace.domain.OfferStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.CompanyRepository;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.repository.PostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** ADM-02. */
@Service
public class ReportService {

    private final ApplicationRepository applications;
    private final OfferRepository offers;
    private final CompanyRepository companies;
    private final PostingRepository postings;

    public ReportService(ApplicationRepository applications, OfferRepository offers, CompanyRepository companies,
                         PostingRepository postings) {
        this.applications = applications;
        this.offers = offers;
        this.companies = companies;
        this.postings = postings;
    }

    public record Table(String title, List<String> header, List<List<String>> rows) {
    }

    @Transactional(readOnly = true)
    public Table placementRate() {
        List<List<String>> rows = new ArrayList<>();
        for (Object[] r : applications.placementByProgram()) {
            long students = ((Number) r[1]).longValue();
            long placed = ((Number) r[2]).longValue();
            rows.add(List.of((String) r[0], String.valueOf(students), String.valueOf(placed), percent(placed, students)));
        }
        return new Table("Placement rate per program", List.of("Program", "Students", "Placed", "Placement rate"), rows);
    }

    @Transactional(readOnly = true)
    public Table averageScorePerCompany() {
        List<List<String>> rows = new ArrayList<>();
        for (Company company : companies.findAllByOrderByNameAsc()) {
            long total = 0;
            long count = 0;
            for (Posting posting : postings.findByCompany(company)) {
                for (Application a : applications.findByPosting(posting)) {
                    total += a.getMatchScore();
                    count++;
                }
            }
            if (count > 0) {
                double avg = (double) total / count;
                rows.add(List.of(company.getName(), BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP)
                        .toPlainString(), String.valueOf(count)));
            }
        }
        return new Table("Average match score per company", List.of("Company", "Average score", "Applications"), rows);
    }

    @Transactional(readOnly = true)
    public Table offersIssuedVersusAccepted() {
        long issued = offers.count();
        long accepted = offers.countByStatus(OfferStatus.ACCEPTED);
        List<List<String>> rows = List.of(List.of(String.valueOf(issued), String.valueOf(accepted),
                percent(accepted, issued)));
        return new Table("Offers issued versus accepted", List.of("Issued", "Accepted", "Acceptance rate"), rows);
    }

    @Transactional(readOnly = true)
    public Table daysToOffer() {
        List<Offer> all = offers.findAllByOrderByIdAsc();
        long days = 0;
        for (Offer o : all) {
            days += ChronoUnit.DAYS.between(o.getApplication().getSubmittedAt().toLocalDate(), o.getIssuedAt().toLocalDate());
        }
        String avg = all.isEmpty() ? "0.0" : BigDecimal.valueOf(days)
                .divide(BigDecimal.valueOf(all.size()), 1, RoundingMode.HALF_UP).toPlainString();
        return new Table("Average days from submission to offer", List.of("Offers", "Average days"),
                List.of(List.of(String.valueOf(all.size()), avg)));
    }

    public List<Table> all() {
        return List.of(placementRate(), averageScorePerCompany(), offersIssuedVersusAccepted(), daysToOffer());
    }

    public Table byName(String name) {
        return switch (name) {
            case "placement" -> placementRate();
            case "scores" -> averageScorePerCompany();
            case "offers" -> offersIssuedVersusAccepted();
            case "days" -> daysToOffer();
            default -> throw new NotFoundException("Report");
        };
    }

    public static String toCsv(Table t) {
        StringBuilder sb = new StringBuilder();
        sb.append(csvLine(t.header()));
        for (List<String> row : t.rows()) {
            sb.append(csvLine(row));
        }
        return sb.toString();
    }

    private static String csvLine(List<String> cells) {
        List<String> escaped = new ArrayList<>();
        for (String c : cells) {
            String v = c == null ? "" : c;
            if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                v = "\"" + v.replace("\"", "\"\"") + "\"";
            }
            escaped.add(v);
        }
        return String.join(",", escaped) + "\r\n";
    }

    private static String percent(long part, long whole) {
        if (whole == 0) {
            return "0.0%";
        }
        return BigDecimal.valueOf(part * 100.0 / whole).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
