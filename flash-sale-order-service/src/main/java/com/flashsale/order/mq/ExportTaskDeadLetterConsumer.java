package com.flashsale.order.mq;

import com.flashsale.common.mq.event.DomainEvent;
import com.flashsale.order.service.ExportTaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ExportTaskDeadLetterConsumer {

    private final ExportTaskService exportTaskService;

    public ExportTaskDeadLetterConsumer(ExportTaskService exportTaskService) {
        this.exportTaskService = exportTaskService;
    }

    @RabbitListener(queues = "${flash-sale.mq.export-generate-dead.queue:flash.sale.export.generate.dead.queue}")
    public void onDeadLetter(DomainEvent<?> event) {
        exportTaskService.recordDeadLetter(ExportDeadLetterPayload.from(event));
    }
}
