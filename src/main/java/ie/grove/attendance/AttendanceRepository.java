package ie.grove.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByChildIdAndDate(Long childId, LocalDate date);

    List<Attendance> findByDate(LocalDate date);

    Optional<Attendance> findFirstByChildIdAndCheckOutIsNullOrderByCheckInAsc(Long childId);

    Optional<Attendance> findFirstByChildIdAndDateOrderByIdAsc(Long childId, LocalDate date);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);
}
