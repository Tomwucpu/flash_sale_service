package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ExportTaskConsumer {

    private final ExportTaskService exportTaskService;

    public ExportTaskConsumer(ExportTaskService exportTaskService) {
        this.exportTaskService = exportTaskService;
    }

    @RabbitListener(queues = "${flash-sale.mq.export-generate.queue:flash.sale.export.generate.queue}")
    public void onExportGenerate(DomainEvent<?> event) {
        exportTaskService.processTask(ExportGeneratePayload.from(event));
    }
}
