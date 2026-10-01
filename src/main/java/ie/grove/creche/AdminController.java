package ie.grove.creche;

import ie.grove.attendance.AttendanceRepository;
import ie.grove.attendance.AttendanceStatus;
import ie.grove.child.ChildRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

  private final ChildRepository children;
  private final AttendanceRepository attendance;
  private final RoomRepository rooms;

  AdminController(ChildRepository children, AttendanceRepository attendance, RoomRepository rooms) {
    this.children = children;
    this.attendance = attendance;
    this.rooms = rooms;
  }

  @GetMapping("/admin")
  String dashboard(Model model) {
    model.addAttribute("pageTitle", "Dashboard");
    model.addAttribute("active", "dashboard");
    model.addAttribute("userName", "Maria Byrne");
    model.addAttribute("childrenCount", children.count());
    model.addAttribute("presentToday", (int) attendance.countByDateAndStatus(LocalDate.now(), AttendanceStatus.PRESENT));
    model.addAttribute("roomsCount", rooms.count());
    return "admin/dashboard";
  }
}
