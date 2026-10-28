
package com.example.rabbitMQDemoBackend.service;

import com.example.rabbitMQDemoBackend.config.RabbitMQConfig;

import java.time.LocalTime;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumerService {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveMessage(String message) {

        System.out.println("\n========================================");
        System.out.println("[CONSUMER] Received: " + message);
        System.out.println("[CONSUMER] Time: " + LocalTime.now());

        // CASE 1: Happy Case - xử lý thành công
        if (message.startsWith("SUCCESS")) {
            System.out.println("[CONSUMER] Processing message...");
            System.out.println("[CONSUMER] Processing successful!");
            System.out.println("[CONSUMER] ACK handled by Spring AMQP");
        }

        // CASE 2: Normal Case - xử lý chậm trong 3 giây
        else if (message.startsWith("SLOW")) {
            System.out.println("[CONSUMER] Processing slowly...");

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Consumer processing was interrupted", e
                );
            }

            System.out.println("[CONSUMER] Completed at: " + LocalTime.now());
            System.out.println("[CONSUMER] Processing successful!");
        }

        // CASE 3: Bad Case - chuyển thẳng sang DLQ
        else if (message.startsWith("FAIL")) {
            System.err.println("[CONSUMER] Processing failed!");
            System.err.println("[CONSUMER] Rejecting without requeue...");

            throw new AmqpRejectAndDontRequeueException(
                    "Demo processing failure"
            );
        }

        // CASE 4: Retry - cố tình thất bại để thử lại
        else if (message.startsWith("RETRY")) {
            System.err.println("[CONSUMER] Temporary error occurred!");
            System.err.println("[CONSUMER] Spring AMQP will retry...");

            throw new RuntimeException(
                    "Simulated temporary processing error"
            );
        }

        // CASE mặc định
        else {
            System.out.println("[CONSUMER] Unknown message type");
        }

        System.out.println("========================================\n");
    }
}
