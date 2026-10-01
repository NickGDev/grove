package ie.grove.billing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Easy fees screen (design.md). Stub data source — swapped for fee_item/subvention/invoice
 * queries once the billing entities land. Week is ISO "2026-W40" form.
 */
@Controller
public class BillingPageController {

  record FeeLine(String description, int amountCents) {}
  record FeeRow(long id, String child, String parents, List<FeeLine> lines, int dueCents, int paidCents) {
    public String status() {
      if (dueCents <= 0 || paidCents >= dueCents) return "PAID";
      if (paidCents > 0) return "PART_PAID";
      return "DUE";
    }
  }

  private static final Map<Long, FeeRow> SEED = Map.of(
    1L, new FeeRow(1, "Maria Jones", "Alison Jones, John Jones",
        List.of(new FeeLine("Full-time, 5 days", 24_500), new FeeLine("ECCE subvention", -7_750),
                new FeeLine("Extra hours", 2_000)), 18_750, 18_750),
    2L, new FeeRow(2, "Aisling Byrne", "Mary Byrne, Paul Byrne",
        List.of(new FeeLine("Part-time, 3 days", 15_000), new FeeLine("NCS subvention", -2_500)), 12_500, 6_000),
    3L, new FeeRow(3, "Leo Wilson", "Sarah Wilson",
        List.of(new FeeLine("Full-time, 5 days", 24_500)), 24_500, 0),
    4L, new FeeRow(4, "Aoife Murphy", "Denise Murphy",
        List.of(new FeeLine("Part-time, 3 days", 15_000)), 15_000, 15_000),
    5L, new FeeRow(5, "Fionn O'Brien", "Niamh O'Brien, Cian O'Brien",
        List.of(new FeeLine("Full-time, 5 days", 24_500), new FeeLine("Sibling discount", -1_500)), 23_000, 0));

  /** Session-scoped working copy so "Record payment" survives across fragment swaps. */
  @ModelAttribute("feeStore")
  Map<Long, FeeRow> feeStore() {
    Map<Long, FeeRow> store = new LinkedHashMap<>();
    SEED.forEach(store::put);
    return store;
  }

  @GetMapping("/admin/fees")
  String fees(@RequestParam(defaultValue = "") String week, @RequestParam(defaultValue = "asc") String sort,
      @ModelAttribute("feeStore") Map<Long, FeeRow> store, Model model) {
    addPanelModel(week, sort, store, model);
    return "admin/fees";
  }

  @GetMapping("/admin/fees/panel")
  String feesPanel(@RequestParam(defaultValue = "") String week, @RequestParam(defaultValue = "asc") String sort,
      @ModelAttribute("feeStore") Map<Long, FeeRow> store, Model model) {
    addPanelModel(week, sort, store, model);
    return "admin/fragments/fees-panel :: panel";
  }

  @PostMapping("/admin/fees/{id}/pay")
  String recordPayment(@PathVariable long id, @RequestParam String week, @RequestParam String sort,
      @ModelAttribute("feeStore") Map<Long, FeeRow> store, HttpServletResponse response, Model model) {
    FeeRow row = store.get(id);
    if (row != null) {
      store.put(id, new FeeRow(id, row.child(), row.parents(), row.lines(), row.dueCents(), row.dueCents()));
    }
    response.addHeader("HX-Trigger", "payment-recorded");
    addPanelModel(week, sort, store, model);
    return "admin/fragments/fees-panel :: panel";
  }

  private void addPanelModel(String week, String sort, Map<Long, FeeRow> store, Model model) {
    LocalDate monday = parseWeek(week);
    DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    model.addAttribute("weekParam", formatWeek(monday));
    model.addAttribute("weekLabel", label.format(monday) + ", Week " + monday.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
    model.addAttribute("prevWeek", formatWeek(monday.minusWeeks(1)));
    model.addAttribute("nextWeek", formatWeek(monday.plusWeeks(1)));
    model.addAttribute("isCurrentWeek", formatWeek(LocalDate.now().with(java.time.DayOfWeek.MONDAY)).equals(formatWeek(monday)));
    model.addAttribute("sort", sort);

    List<FeeRow> rows = new ArrayList<>(store.values());
    Comparator<FeeRow> byName = Comparator.comparing(r -> r.child(), String.CASE_INSENSITIVE_ORDER);
    rows.sort("desc".equals(sort) ? byName.reversed() : byName);
    model.addAttribute("rows", rows);

    int totalOutstanding = rows.stream().mapToInt(r -> Math.max(0, r.dueCents() - r.paidCents())).sum();
    int totalPaid = rows.stream().mapToInt(FeeRow::paidCents).sum();
    model.addAttribute("totalOutstanding", totalOutstanding);
    model.addAttribute("totalPaid", totalPaid);
  }

  private static LocalDate parseWeek(String week) {
    try {
      if (!week.isBlank()) {
        String[] parts = week.split("-W");
        return LocalDate.of(Integer.parseInt(parts[0]), 1, 1)
            .with(java.time.temporal.WeekFields.ISO.weekBasedYear(), Integer.parseInt(parts[0]))
            .with(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear(), Integer.parseInt(parts[1]))
            .with(java.time.DayOfWeek.MONDAY);
      }
    } catch (Exception ignored) { }
    return LocalDate.now().with(java.time.DayOfWeek.MONDAY);
  }

  private static String formatWeek(LocalDate monday) {
    int year = monday.get(java.time.temporal.WeekFields.ISO.weekBasedYear());
    int wk = monday.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    return year + "-W" + (wk < 10 ? "0" + wk : wk);
  }
}
