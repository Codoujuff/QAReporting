package com.qareporting.resource;

import com.qareporting.security.CurrentUser;
import com.qareporting.service.ReportingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

@Path("/reporting")
@Produces(MediaType.APPLICATION_JSON)
public class ReportingResource {

    @Inject
    ReportingService reportingService;

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/dashboard")
    public Response dashboard() {
        return Response.ok(reportingService.dashboard(currentUser.get()).toString()).build();
    }

    @GET
    @Path("/daily")
    public Response daily(@QueryParam("date") String date) {
        LocalDate day = date != null ? LocalDate.parse(date) : LocalDate.now();
        return Response.ok(reportingService.daily(currentUser.get(), day).toString()).build();
    }

    @GET
    @Path("/weekly")
    public Response weekly(@QueryParam("week_start") String weekStart) {
        LocalDate start = weekStart != null
                ? LocalDate.parse(weekStart)
                : LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return Response.ok(reportingService.weekly(currentUser.get(), start).toString()).build();
    }

    @GET
    @Path("/monthly")
    public Response monthly(@QueryParam("month") String month) {
        YearMonth yearMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
        return Response.ok(reportingService.monthly(currentUser.get(), yearMonth).toString()).build();
    }
}
