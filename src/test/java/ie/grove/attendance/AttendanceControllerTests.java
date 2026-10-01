package ie.grove.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ie.grove.child.Child;
import ie.grove.child.ChildRepository;
import ie.grove.creche.Creche;
import ie.grove.creche.CrecheRepository;
import ie.grove.shared.TenantContext;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Register endpoints against the seeded creche (V2, id 1). WithMockUser's
 * principal is not a GroveUserDetails, so TenantContextFilter leaves the
 * thread's tenant untouched but clears it after every request — tests re-set
 * creche 1 before each request and each assertion (see {@link #perform}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AttendanceControllerTests {

    @Autowired MockMvc mvc;
    @Autowired AttendanceRepository attendance;
    @Autowired ChildRepository children;
    @Autowired CrecheRepository creches;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void actAsCrecheOne() {
        TenantContext.set(1L);
        attendance.deleteAll(attendance.findAll());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    /** TenantContextFilter clears the tenant in its finally block — re-set it. */
    private ResultActions perform(RequestBuilder request) throws Exception {
        TenantContext.set(1L);
        return mvc.perform(request);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void pageAndPanelRenderSeededRooms() throws Exception {
        perform(get("/admin/attendance"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Attendance")));

        perform(get("/admin/attendance/panel").param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nestlings")))
                .andExpect(content().string(containsString("Wobblers")))
                .andExpect(content().string(containsString("Explorers")))
                .andExpect(content().string(containsString("Pre-school")))
                .andExpect(content().string(containsString("Maria Jones")))
                .andExpect(content().string(containsString("Expected")));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void checkInCreatesPresentRowAndTriggersUpdate() throws Exception {
        perform(post("/admin/attendance/1/check-in").param("date", today.toString()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger", "attendance-updated"))
                .andExpect(content().string(containsString("Wobblers")));

        TenantContext.set(1L);
        List<Attendance> rows = attendance.findByChildIdAndDate(1L, today);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(rows.get(0).getCheckIn()).isNotNull();
        assertThat(rows.get(0).getCheckOut()).isNull();
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void checkInTwiceKeepsSingleRowWithOriginalCheckIn() throws Exception {
        perform(post("/admin/attendance/1/check-in").param("date", today.toString()).with(csrf()))
                .andExpect(status().isOk());

        TenantContext.set(1L);
        LocalTime first = attendance.findFirstByChildIdAndDateOrderByIdAsc(1L, today).orElseThrow().getCheckIn();

        perform(post("/admin/attendance/1/check-in").param("date", today.toString()).with(csrf()))
                .andExpect(status().isOk());

        TenantContext.set(1L);
        List<Attendance> rows = attendance.findByChildIdAndDate(1L, today);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCheckIn()).isEqualTo(first);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void checkOutAfterCheckInRecordsTime() throws Exception {
        perform(post("/admin/attendance/2/check-in").param("date", today.toString()).with(csrf()))
                .andExpect(status().isOk());
        perform(post("/admin/attendance/2/check-out").param("date", today.toString()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger", "attendance-updated"));

        TenantContext.set(1L);
        Attendance row = attendance.findFirstByChildIdAndDateOrderByIdAsc(2L, today).orElseThrow();
        assertThat(row.getCheckIn()).isNotNull();
        assertThat(row.getCheckOut()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void absentSetsStatusAndReason() throws Exception {
        perform(post("/admin/attendance/3/absent").param("date", today.toString())
                        .param("reason", "Fever").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger", "attendance-updated"));

        TenantContext.set(1L);
        Attendance row = attendance.findFirstByChildIdAndDateOrderByIdAsc(3L, today).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(row.getAbsenceReason()).isEqualTo("Fever");
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void unknownChildReturns404() throws Exception {
        perform(post("/admin/attendance/9999/check-in").param("date", today.toString()).with(csrf()))
                .andExpect(status().isNotFound());

        TenantContext.set(1L);
        assertThat(attendance.findByDate(today)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void childFromAnotherCrecheReturns404() throws Exception {
        TenantContext.clear();
        Long crecheB = creches.save(new Creche("Attendance Test Creche B")).getId();
        TenantContext.set(crecheB);
        Long childB = children.saveAndFlush(new Child("Zoe", "Tester", LocalDate.of(2024, 1, 5))).getId();
        TenantContext.set(1L);

        perform(post("/admin/attendance/" + childB + "/check-in").param("date", today.toString()).with(csrf()))
                .andExpect(status().isNotFound());

        TenantContext.set(1L);
        assertThat(attendance.findByChildIdAndDate(childB, today)).isEmpty();
    }
}
