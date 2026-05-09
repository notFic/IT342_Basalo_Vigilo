package edu.cit.basalo.vigilo.features.location;

import jakarta.persistence.*;

@Entity
@Table(name = "locations")
public class Location {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String areaName;
    @Column(nullable = false) private String roomNumber;
    @Column(nullable = false) private String floorLevel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getFloorLevel() { return floorLevel; }
    public void setFloorLevel(String floorLevel) { this.floorLevel = floorLevel; }
}
