package com.qareporting.dto;

import com.qareporting.entity.Test;

public class ExecuteTestRequest {
    private Test.Status status;
    private String actualResult;
    private Long environmentId;
    private String duration;

    public Test.Status getStatus() { return status; }
    public void setStatus(Test.Status status) { this.status = status; }
    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }
    public Long getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(Long environmentId) { this.environmentId = environmentId; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
}
