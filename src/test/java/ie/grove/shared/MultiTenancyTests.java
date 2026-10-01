package ie.grove.shared;

import ie.grove.attendance.Attendance;
import ie.grove.attendance.AttendanceRepository;
import ie.grove.attendance.AttendanceStatus;
import ie.grove.child.Child;
import ie.grove.child.ChildRepository;
import ie.grove.creche.Creche;
import ie.grove.creche.CrecheRepository;
import ie.grove.creche.Room;
import ie.grove.creche.RoomRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flyway schema + Hibernate mapping + @TenantId filtering against the real
 * SQLite datasource. NOT_SUPPORTED so every repository call gets its own
 * session — the tenant filter binds to the session, so isolation must be
 * queried in a session opened after TenantContext is set.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TenancyConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MultiTenancyTests {

    @Autowired
    CrecheRepository crecheRepository;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    ChildRepository childRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void findAllReturnsOnlyCurrentCrecheChildren() {
        Long crecheA = crecheRepository.save(new Creche("Sunshine Creche A")).getId();
        Long crecheB = crecheRepository.save(new Creche("Sunshine Creche B")).getId();

        TenantContext.set(crecheA);
        Long childA = childRepository
                .saveAndFlush(new Child("Ada", "Byrne", LocalDate.of(2024, 4, 1)))
                .getId();
        TenantContext.set(crecheB);
        Long childB = childRepository
                .saveAndFlush(new Child("Ben", "Byrne", LocalDate.of(2024, 5, 2)))
                .getId();

        TenantContext.set(crecheA);
        List<Child> visible = childRepository.findAll();
        org.junit.jupiter.api.Assertions.assertEquals(1, visible.size());
        org.junit.jupiter.api.Assertions.assertEquals(childA, visible.get(0).getId());
        org.junit.jupiter.api.Assertions.assertEquals(crecheA, visible.get(0).getCrecheId());

        // No tenant context means no filter — creche B's child is still in the DB.
        TenantContext.clear();
        List<Long> unfiltered = childRepository.findAll().stream().map(Child::getId).toList();
        org.junit.jupiter.api.Assertions.assertTrue(unfiltered.contains(childA));
        org.junit.jupiter.api.Assertions.assertTrue(unfiltered.contains(childB));
    }

    @Test
    void childResolvesThroughRoomToCreche() {
        Long crecheId = crecheRepository.save(new Creche("Ashgrove Montessori")).getId();

        TenantContext.set(crecheId);
        Room room = roomRepository.saveAndFlush(new Room("Wobblers", 8, 12, 24));
        Child child = childRepository.saveAndFlush(new Child("Saoirse", "Kelly", LocalDate.of(2025, 3, 14)));
        child.setRoom(room);
        child = childRepository.saveAndFlush(child);

        Child loaded = childRepository.findById(child.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Wobblers", loaded.getRoom().getName());
        org.junit.jupiter.api.Assertions.assertEquals(crecheId, loaded.getRoom().getCrecheId());
        org.junit.jupiter.api.Assertions.assertEquals("Ashgrove Montessori",
                crecheRepository.findById(loaded.getRoom().getCrecheId()).orElseThrow().getName());

        Attendance attendance = attendanceRepository.saveAndFlush(
                new Attendance(child.getId(), LocalDate.of(2026, 9, 30), AttendanceStatus.PRESENT));
        attendance.setCheckIn(LocalTime.of(8, 5));
        attendance = attendanceRepository.saveAndFlush(attendance);

        Attendance reloaded = attendanceRepository.findById(attendance.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(child.getId(), reloaded.getChildId());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(8, 5), reloaded.getCheckIn());
        org.junit.jupiter.api.Assertions.assertEquals(crecheId, reloaded.getCrecheId());
    }
}
