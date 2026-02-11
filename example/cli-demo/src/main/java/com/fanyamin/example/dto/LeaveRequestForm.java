package com.fanyamin.example.dto;

import com.fanyamin.instructor.schema.*;

/**
 * DTO for Leave Request form.
 * This class is used to generate JSON Schema for form validation.
 */
public class LeaveRequestForm {

    @SchemaRequired
    @SchemaEnum({"annual", "sick", "unpaid"})
    @SchemaDescription("Type of leave request")
    private String leaveType;

    @SchemaRequired
    @SchemaFormat("date")
    @SchemaDescription("Start date of leave")
    private String startDate;

    @SchemaRequired
    @SchemaFormat("date")
    @SchemaDescription("End date of leave")
    private String endDate;

    @SchemaRequired
    @SchemaDescription("Reason for leave")
    private String reason;

    @SchemaDescription("Medical certificate if required")
    private String medicalCertificate;

    @SchemaDescription("Person who will approve the leave")
    private String approver;

    // Getters and setters (optional, schema generation uses fields)
    
    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMedicalCertificate() {
        return medicalCertificate;
    }

    public void setMedicalCertificate(String medicalCertificate) {
        this.medicalCertificate = medicalCertificate;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }
}

