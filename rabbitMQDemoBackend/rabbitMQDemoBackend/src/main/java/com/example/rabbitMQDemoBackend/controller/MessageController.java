
package com.example.rabbitMQDemoBackend.controller;

import com.example.rabbitMQDemoBackend.dto.MessageRequest;
import com.example.rabbitMQDemoBackend.service.MessageProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageProducerService producerService;

    public MessageController(MessageProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> sendMessage(@RequestBody MessageRequest request) {
        producerService.sendMessage(request.message());

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message", "Message sent to RabbitMQ",
                        "data", request.message()));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> sendBulkMessages(
            @RequestParam(defaultValue = "10") int count) {

        if (count < 1 || count > 100) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "count must be between 1 and 100"));
        }

        for (int i = 1; i <= count; i++) {
            producerService.sendMessage("Demo message #" + i);
        }

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "sentCount", count,
                        "message", "Messages published to RabbitMQ"));
    }

}
