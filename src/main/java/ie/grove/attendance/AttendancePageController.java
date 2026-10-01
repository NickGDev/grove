package ie.grove.attendance;

import ie.grove.child.Child;
import ie.grove.child.ChildRepository;
import ie.grove.creche.Room;
import ie.grove.creche.RoomRepository;
import ie.grove.shared.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Attendance register (spec.md §7): today's room register with one-tap
 * check-in, check-out and absent. Every endpoint takes the date param so the
 * week/history view can reuse the same code path in a later chunk.
 */
@Controller
public class AttendancePageController {

    /** Public records with public accessors — Thymeleaf/SpEL needs the getters. */
    public record ChildAttendance(long childId, String name, String status,
            LocalTime checkIn, LocalTime checkOut, String absenceReason) {
    }

    public record RoomRegister(long roomId, String name, int capacity, List<ChildAttendance> children) {
    }

    private final RoomRepository rooms;
    private final ChildRepository children;
    private final AttendanceRepository attendance;

    AttendancePageController(RoomRepository rooms, ChildRepository children, AttendanceRepository attendance) {
        this.rooms = rooms;
        this.children = children;
        this.attendance = attendance;
    }

    @GetMapping("/admin/attendance")
    String page(@RequestParam(defaultValue = "") String date, Model model) {
        model.addAttribute("pageTitle", "Attendance");
        model.addAttribute("active", "attendance");
        model.addAttribute("userName", "Maria Byrne");
        addPanelModel(parseDate(date), model);
        return "admin/attendance";
    }

    @GetMapping("/admin/attendance/panel")
    String panel(@RequestParam(defaultValue = "") String date, Model model) {
        addPanelModel(parseDate(date), model);
        return "admin/fragments/attendance-panel :: panel";
    }

    @PostMapping("/admin/attendance/{childId}/check-in")
    String checkIn(@PathVariable long childId, @RequestParam(defaultValue = "") String date,
            HttpServletResponse response, Model model) {
        LocalDate day = parseDate(date);
        requireCrecheChild(childId);
        Attendance row = attendance.findFirstByChildIdAndDateOrderByIdAsc(childId, day).orElse(null);
        if (row == null) {
            row = new Attendance(childId, day, AttendanceStatus.PRESENT);
            row.setCheckIn(now());
        } else if (row.getStatus() != AttendanceStatus.PRESENT) {
            // Absent or holiday tap-in: late arrival, times restart.
            row.setStatus(AttendanceStatus.PRESENT);
            row.setCheckIn(now());
            row.setCheckOut(null);
            row.setAbsenceReason(null);
        }
        attendance.save(row);
        return swap(date, response, model);
    }

    @PostMapping("/admin/attendance/{childId}/check-out")
    String checkOut(@PathVariable long childId, @RequestParam(defaultValue = "") String date,
            HttpServletResponse response, Model model) {
        requireCrecheChild(childId);
        Attendance row = attendance.findFirstByChildIdAndDateOrderByIdAsc(childId, parseDate(date)).orElse(null);
        if (row != null && row.getCheckOut() == null) {
            row.setCheckOut(now());
            attendance.save(row);
        }
        return swap(date, response, model);
    }

    @PostMapping("/admin/attendance/{childId}/absent")
    String markAbsent(@PathVariable long childId, @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "") String reason, HttpServletResponse response, Model model) {
        LocalDate day = parseDate(date);
        requireCrecheChild(childId);
        Attendance row = attendance.findFirstByChildIdAndDateOrderByIdAsc(childId, day).orElse(null);
        if (row == null) {
            row = new Attendance(childId, day, AttendanceStatus.ABSENT);
        }
        row.setStatus(AttendanceStatus.ABSENT);
        row.setAbsenceReason(reason.isBlank() ? null : reason);
        row.setCheckIn(null);
        row.setCheckOut(null);
        attendance.save(row);
        return swap(date, response, model);
    }

    private String swap(String date, HttpServletResponse response, Model model) {
        response.addHeader("HX-Trigger", "attendance-updated");
        addPanelModel(parseDate(date), model);
        return "admin/fragments/attendance-panel :: panel";
    }

    /** 404 before any mutation for unknown ids and other creches' children. */
    private void requireCrecheChild(long childId) {
        children.findById(childId)
                .filter(child -> child.getCrecheId().equals(TenantContext.get()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void addPanelModel(LocalDate day, Model model) {
        Map<Long, Attendance> rowsByChild = attendance.findByDate(day).stream()
                .collect(Collectors.toMap(Attendance::getChildId, Function.identity(), (first, second) -> first));

        List<RoomRegister> registers = new ArrayList<>();
        int present = 0;
        int absent = 0;
        int expected = 0;
        for (Room room : rooms.findAll(Sort.by(Sort.Direction.ASC, "id"))) {
            List<ChildAttendance> kids = children.findByRoomIdOrderByIdAsc(room.getId()).stream()
                    .filter(child -> enrolled(child, day))
                    .map(child -> toChildAttendance(child, rowsByChild))
                    .sorted(Comparator.comparing(ChildAttendance::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            expected += kids.size();
            present += (int) kids.stream().filter(kid -> "PRESENT".equals(kid.status())).count();
            absent += (int) kids.stream().filter(kid -> "ABSENT".equals(kid.status())).count();
            registers.add(new RoomRegister(room.getId(), room.getName(), room.getCapacity(), kids));
        }

        model.addAttribute("date", day);
        model.addAttribute("dateLabel", DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH).format(day));
        model.addAttribute("isToday", day.equals(LocalDate.now()));
        model.addAttribute("rooms", registers);
        model.addAttribute("presentCount", present);
        model.addAttribute("absentCount", absent);
        model.addAttribute("expectedCount", expected);
    }

    private static ChildAttendance toChildAttendance(Child child, Map<Long, Attendance> rowsByChild) {
        Attendance row = rowsByChild.get(child.getId());
        return new ChildAttendance(child.getId(),
                child.getFirstName() + " " + child.getLastName(),
                row != null ? row.getStatus().name() : "NOT_IN",
                row != null ? row.getCheckIn() : null,
                row != null ? row.getCheckOut() : null,
                row != null ? row.getAbsenceReason() : null);
    }

    /** Enrolled on the day: started on or before it, not yet left. */
    private static boolean enrolled(Child child, LocalDate day) {
        return (child.getStartDate() == null || !child.getStartDate().isAfter(day))
                && (child.getEndDate() == null || !child.getEndDate().isBefore(day));
    }

    private static LocalTime now() {
        return LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static LocalDate parseDate(String date) {
        try {
            if (!date.isBlank()) {
                return LocalDate.parse(date);
            }
        } catch (Exception ignored) { }
        return LocalDate.now();
    }
}
