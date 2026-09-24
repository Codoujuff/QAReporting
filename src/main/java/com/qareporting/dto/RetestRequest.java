package com.qareporting.dto;

public class RetestRequest {
    private boolean passed;
    private String comment;

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
