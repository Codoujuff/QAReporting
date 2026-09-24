package com.qareporting.resource;

import com.qareporting.security.CurrentUser;
import com.qareporting.service.ExportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/exports")
public class ExportResource {

    private static final String XLSX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    ExportService exportService;

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/activities")
    public Response activities(@QueryParam("format") String format) {
        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        byte[] body = xlsx ? exportService.activitiesXlsx(currentUser.get()) : exportService.activitiesCsv(currentUser.get());
        return export("activities", xlsx, body);
    }

    @GET
    @Path("/defects")
    public Response defects(@QueryParam("format") String format) {
        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        byte[] body = xlsx ? exportService.defectsXlsx(currentUser.get()) : exportService.defectsCsv(currentUser.get());
        return export("defects", xlsx, body);
    }

    @GET
    @Path("/campaigns")
    public Response campaigns(@QueryParam("format") String format) {
        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        byte[] body = xlsx ? exportService.campaignsXlsx(currentUser.get()) : exportService.campaignsCsv(currentUser.get());
        return export("campaigns", xlsx, body);
    }

    private Response export(String baseName, boolean xlsx, byte[] body) {
        String mediaType = xlsx ? XLSX_MEDIA_TYPE : "text/csv;charset=UTF-8";
        String extension = xlsx ? "xlsx" : "csv";

        return Response.ok(body, mediaType)
                .header("Content-Disposition", "attachment; filename=\"" + baseName + "." + extension + "\"")
                .build();
    }
}
