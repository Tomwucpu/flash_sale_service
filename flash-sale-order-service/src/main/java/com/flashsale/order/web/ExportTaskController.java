package com.flashsale.order.web;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.order.application.ExportTaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exports")
public class ExportTaskController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ExportTaskService exportTaskService;

    public ExportTaskController(ExportTaskService exportTaskService) {
        this.exportTaskService = exportTaskService;
    }

    @PostMapping("/tasks")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ExportTaskResponse> createTask(
            @Valid @RequestBody ExportTaskCreateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        ExportTaskService.ExportTaskView view = exportTaskService.createTask(
                new ExportTaskService.ExportTaskCreateCommand(
                        request.activityId(),
                        request.format(),
                        request.filters(),
                        UserContextHolder.get().userId(),
                        requestId(httpServletRequest)
                )
        );
        return ApiResponse.success(requestId(httpServletRequest), toResponse(view));
    }

    @GetMapping("/tasks/{taskId}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<ExportTaskResponse> getTask(
            @PathVariable Long taskId,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(requestId(httpServletRequest), toResponse(exportTaskService.getTask(taskId)));
    }

    @GetMapping("/tasks")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<List<ExportTaskResponse>> listTasks(
            @RequestParam Long activityId,
            HttpServletRequest httpServletRequest
    ) {
        List<ExportTaskResponse> responses = exportTaskService.listTasks(activityId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(requestId(httpServletRequest), responses);
    }

    @GetMapping("/compensations")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<List<CompensationRecordResponse>> listCompensations(HttpServletRequest httpServletRequest) {
        List<CompensationRecordResponse> responses = exportTaskService.listCompensations()
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(requestId(httpServletRequest), responses);
    }

    @PostMapping("/compensations/{compensationId}/resolve")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ApiResponse<CompensationRecordResponse> resolveCompensation(
            @PathVariable Long compensationId,
            @Valid @RequestBody CompensationResolveRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                toResponse(exportTaskService.resolveCompensation(
                        compensationId,
                        request.resolutionNote(),
                        UserContextHolder.get(),
                        requestId(httpServletRequest)
                ))
        );
    }

    @GetMapping("/files/{fileName}")
    @RequireRole({"ADMIN", "PUBLISHER"})
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        return exportTaskService.downloadFile(fileName);
    }

    private ExportTaskResponse toResponse(ExportTaskService.ExportTaskView view) {
        return new ExportTaskResponse(
                view.id(),
                view.activityId(),
                view.operatorId(),
                view.format(),
                view.filters(),
                view.status(),
                view.fileUrl(),
                view.failReason(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private CompensationRecordResponse toResponse(ExportTaskService.CompensationRecordView view) {
        return new CompensationRecordResponse(
                view.id(),
                view.bizType(),
                view.bizKey(),
                view.sourceEvent(),
                view.status(),
                view.reason(),
                view.resolutionNote(),
                view.resolvedAt(),
                view.createdAt()
        );
    }

    private String requestId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(REQUEST_ID_HEADER);
    }
}
