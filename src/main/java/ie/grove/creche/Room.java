package ie.grove.creche;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "room")
public class Room extends TenantEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "age_min_months", nullable = false)
    private int ageMinMonths;

    @Column(name = "age_max_months", nullable = false)
    private int ageMaxMonths;

    protected Room() {
    }

    public Room(String name, int capacity, int ageMinMonths, int ageMaxMonths) {
        this.name = name;
        this.capacity = capacity;
        this.ageMinMonths = ageMinMonths;
        this.ageMaxMonths = ageMaxMonths;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getAgeMinMonths() {
        return ageMinMonths;
    }

    public void setAgeMinMonths(int ageMinMonths) {
        this.ageMinMonths = ageMinMonths;
    }

    public int getAgeMaxMonths() {
        return ageMaxMonths;
    }

    public void setAgeMaxMonths(int ageMaxMonths) {
        this.ageMaxMonths = ageMaxMonths;
    }
}
