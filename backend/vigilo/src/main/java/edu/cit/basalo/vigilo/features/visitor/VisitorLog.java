package edu.cit.basalo.vigilo.features.visitor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_logs")
public class VisitorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column(name = "host_name", nullable = false)
    private String hostName;

    @Column(name = "visitor_type", nullable = false)
    private String visitorType;

    @Column(name = "destination_room", nullable = false)
    private String destinationRoom;

    @Column(nullable = false, length = 1000)
    private String purpose;

    @Column(name = "extended_visit", nullable = false)
    private boolean extendedVisit;

    @Column(name = "id_image_path", nullable = false)
    private String idImagePath;

    @Column(nullable = false)
    private String status;

    @Column(name = "time_in", nullable = false, updatable = false)
    private LocalDateTime timeIn;

    @Column(name = "time_out")
    private LocalDateTime timeOut;

    @Column(name = "created_by_email", nullable = false)
    private String createdByEmail;

    @Column(name = "updated_by_email")
    private String updatedByEmail;

    @PrePersist
    protected void onCreate() {
        this.timeIn = LocalDateTime.now();
        if (this.status == null || this.status.isBlank()) {
            this.status = "Active";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getVisitorType() {
        return visitorType;
    }

    public void setVisitorType(String visitorType) {
        this.visitorType = visitorType;
    }

    public String getDestinationRoom() {
        return destinationRoom;
    }

    public void setDestinationRoom(String destinationRoom) {
        this.destinationRoom = destinationRoom;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public boolean isExtendedVisit() {
        return extendedVisit;
    }

    public void setExtendedVisit(boolean extendedVisit) {
        this.extendedVisit = extendedVisit;
    }

    public String getIdImagePath() {
        return idImagePath;
    }

    public void setIdImagePath(String idImagePath) {
        this.idImagePath = idImagePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(LocalDateTime timeIn) {
        this.timeIn = timeIn;
    }

    public LocalDateTime getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(LocalDateTime timeOut) {
        this.timeOut = timeOut;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public String getUpdatedByEmail() {
        return updatedByEmail;
    }

    public void setUpdatedByEmail(String updatedByEmail) {
        this.updatedByEmail = updatedByEmail;
    }
}
