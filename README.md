# RabbitMQ Monitoring Demo

Demo hệ thống xử lý message bất đồng bộ sử dụng **Spring Boot, Java và RabbitMQ**. Dự án minh họa cách Producer gửi message vào RabbitMQ, Worker nhận và xử lý message, cơ chế ACK, Retry, Dead Letter Exchange (DLX) và Dead Letter Queue (DLQ).

## 1. Công nghệ sử dụng

- Java 22
- Spring Boot
- Spring AMQP
- RabbitMQ 4 với Management Plugin
- Maven
- Docker và Docker Compose
- Postman để kiểm thử API

## 2. Kiến trúc hệ thống

```text
Postman / Frontend
        |
        v
Spring Boot API (Port 8080)
        |
        v
Message Producer
        |
        v
RabbitMQ Exchange
        |
        v
demo.queue
        |
        v
RabbitMQ Worker (Spring Boot)
        |
        +---- Success ---> ACK
        |
        +---- Retry -----> Retry theo cấu hình
        |
        +---- Failure ---> Reject ---> DLX ---> demo.dlq
```

### Thành phần chính

| Thành phần | Vai trò |
|---|---|
| Spring Boot API | Nhận HTTP request và gửi message vào RabbitMQ |
| Message Producer | Publish message vào Exchange |
| RabbitMQ Exchange | Định tuyến message đến queue |
| `demo.queue` | Queue chứa message chờ xử lý |
| RabbitMQ Worker | Nhận và xử lý message độc lập với API |
| ACK | Xác nhận message đã được xử lý thành công |
| Retry | Thử xử lý lại khi xảy ra lỗi tạm thời |
| DLX | Exchange tiếp nhận message bị dead-letter |
| `demo.dlq` | Queue lưu message bị dead-letter |

## 3. Cấu trúc thư mục

```text
RabbitMQSpring/
├── README.md
├── docker-compose.yml
│
├── rabbitMQDemoBackend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/
│       │   └── com/example/rabbitMQDemoBackend/
│       │       ├── controller/
│       │       │   └── MessageController.java
│       │       ├── service/
│       │       │   ├── MessageProducerService.java
│       │       │   └── MessageConsumerService.java
│       │       ├── config/
│       │       │   └── RabbitMQConfig.java
│       │       └── dto/
│       │           └── MessageRequest.java
│       └── resources/
│           └── application.properties
│
└── rabbitmq-worker/ (TÔI CHƯA LÀM CÁI NÀY ÂN CÓ THỂ CODE THÊM WORKER TẠI ĐÂY)
    ├── pom.xml
    └── src/main/
        ├── java/
        │   └── com/example/rabbitmqworker/
        │       ├── RabbitMqWorkerApplication.java
        │       ├── config/
        │       │   └── RabbitMQConfig.java
        │       └── worker/
        │           └── MessageWorker.java
        └── resources/
            └── application.properties
```

> Cấu trúc trên là cấu trúc mục tiêu. Nếu project thực tế có tên package hoặc thư mục khác, hãy giữ nguyên cấu trúc đang sử dụng và điều chỉnh hướng dẫn tương ứng.

## 4. Yêu cầu môi trường

Cài đặt các công cụ sau trước khi chạy dự án:

- JDK tương thích với phiên bản Java được cấu hình trong `pom.xml`.
- Maven hoặc Maven Wrapper (`mvnw`).
- Docker Desktop.
- Postman hoặc công cụ gửi HTTP request.

Kiểm tra môi trường:

```bash
java -version
mvn -version
docker --version
docker compose version
```

## 5. Khởi chạy RabbitMQ

### Bước 1: Cấu hình Docker Compose

File `docker-compose.yml` ở thư mục gốc:

```yaml
services:
  rabbitmq:
    image: rabbitmq:4-management
    container_name: rabbitmq-demo
    hostname: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: demo
      RABBITMQ_DEFAULT_PASS: demo123
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  rabbitmq_data:
```

Nếu đã có file Docker Compose đang chạy ổn định, có thể giữ nguyên file hiện tại thay vì thay thế.

### Bước 2: Khởi chạy container

Mở terminal tại thư mục chứa `docker-compose.yml`:

```bash
docker compose up -d
```

Kiểm tra container:

```bash
docker ps
```

Xem log nếu RabbitMQ chưa khởi động thành công:

```bash
docker logs rabbitmq-demo
```

### Bước 3: Mở RabbitMQ Management UI

Truy cập:

http://localhost:15672

Thông tin đăng nhập mặc định của demo:

| Trường | Giá trị |
|---|---|
| Username | `demo` |
| Password | `demo123` |
| AMQP Port | `5672` |
| Management Port | `15672` |

Sau khi đăng nhập, mở tab **Queues and Streams** để theo dõi queue và số lượng message.

## 6. Cấu hình Spring Boot API

Trong file `rabbitMQDemoBackend/src/main/resources/application.properties`:

```properties
spring.application.name=rabbitMQDemoBackend

server.port=8080

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=demo
spring.rabbitmq.password=demo123
```

API sử dụng thông tin RabbitMQ tương ứng với Docker Compose ở trên.

### Chạy API

Mở terminal tại thư mục `rabbitMQDemoBackend`:

```bash
mvn spring-boot:run
```

Hoặc chạy class `RabbitMqDemoBackendApplication` trong IDE.

Khi ứng dụng khởi động thành công, API chạy tại:

http://localhost:8080

## 7. Cấu hình và chạy Worker

Worker là một ứng dụng Spring Boot riêng, kết nối trực tiếp tới RabbitMQ và consume message từ `demo.queue`.

Trong `rabbitmq-worker/src/main/resources/application.properties`:

```properties
spring.application.name=rabbitmq-worker

server.port=8081

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=demo
spring.rabbitmq.password=demo123

spring.rabbitmq.listener.simple.acknowledge-mode=auto

spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.initial-interval=2s
spring.rabbitmq.listener.simple.retry.multiplier=2
spring.rabbitmq.listener.simple.retry.max-interval=10s
spring.rabbitmq.listener.simple.retry.max-attempts=3

spring.rabbitmq.listener.simple.default-requeue-rejected=false
```

> Các thuộc tính retry cần được cấu hình trong ứng dụng thực sự đăng ký listener. Nếu bạn vẫn để `MessageConsumerService` trong API consume queue, cấu hình retry của Worker không tự động áp dụng cho listener đó.

### Chạy Worker

Mở terminal mới tại thư mục `rabbitmq-worker`:

```bash
mvn spring-boot:run
```

Khi Worker chạy thành công, log sẽ hiển thị thông tin khởi động Spring Boot và listener kết nối tới RabbitMQ.

**Lưu ý quan trọng:** Nếu muốn Worker riêng chịu trách nhiệm xử lý message, hãy tắt hoặc bỏ `@RabbitListener` của `MessageConsumerService` trong ứng dụng API. Nếu không, cả hai ứng dụng có thể cùng consume một queue.

## 8. Kiểm thử API bằng Postman

### Endpoint gửi message

```http
POST http://localhost:8080/api/messages
Content-Type: application/json
```

Body chọn `raw` → `JSON`.

Ví dụ:

```json
{
  "message": "SUCCESS Hello RabbitMQ"
}
```

Response dự kiến từ API:

```json
{
  "status": "SUCCESS",
  "message": "Message sent to RabbitMQ",
  "data": "SUCCESS Hello RabbitMQ"
}
```

Response trên chỉ xác nhận API đã thực hiện thao tác publish theo code hiện tại; nó không đảm bảo Worker đã xử lý message thành công.

### Kịch bản 1: SUCCESS — Xử lý thành công

Request body:

```json
{
  "message": "SUCCESS Hello RabbitMQ"
}
```

Kết quả dự kiến:

1. API gửi message vào Exchange.
2. RabbitMQ định tuyến message đến `demo.queue`.
3. Worker nhận và xử lý message.
4. Listener kết thúc bình thường.
5. Spring AMQP ACK message theo cấu hình AUTO.

Quan sát log của Worker và trạng thái queue trong Management UI.

### Kịch bản 2: SLOW — Xử lý chậm

Request body:

```json
{
  "message": "SLOW Hello RabbitMQ"
}
```

Worker mô phỏng xử lý trong 5 giây.

Trong thời gian xử lý, message thường xuất hiện trong chỉ số **Unacked** của queue vì listener chưa xác nhận hoàn tất.

Sau khi xử lý thành công, message được ACK.

### Kịch bản 3: FAIL — Chuyển message vào DLQ

Request body:

```json
{
  "message": "FAIL Simulated processing failure"
}
```

Worker ném `AmqpRejectAndDontRequeueException`, yêu cầu reject message mà không requeue.

Nếu queue đã được cấu hình DLX và routing key đúng, message sẽ được chuyển tới:

- Dead Letter Exchange: `demo.dlx`
- Dead Letter Queue: `demo.dlq`

Mở RabbitMQ Management UI → **Queues and Streams** → `demo.dlq` để kiểm tra message.

Với exception này, mục tiêu là reject ngay thay vì thử lại theo retry thông thường. Hành vi thực tế vẫn cần được kiểm tra với cấu hình listener và phiên bản Spring AMQP đang dùng.

### Kịch bản 4: RETRY — Thử xử lý lại

Request body:

```json
{
  "message": "RETRY Simulated temporary error"
}
```

Worker ném `RuntimeException` để kích hoạt cơ chế retry của Spring AMQP.

Với cấu hình:

```properties
spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.max-attempts=3
```

Spring AMQP thực hiện tối đa ba lần xử lý, bao gồm lần đầu.

Sau khi hết số lần thử, message bị từ chối và có thể được chuyển vào DLQ nếu cấu hình DLX hợp lệ.

Đây là retry ở phía ứng dụng Spring AMQP, không phải retry tự động do RabbitMQ broker thực hiện.

## 9. Kiểm tra trực tiếp trên RabbitMQ Management UI

### Queue `demo.queue`

Theo dõi các chỉ số:

| Chỉ số | Ý nghĩa |
|---|---|
| Ready | Message đang chờ consumer nhận |
| Unacked | Message đã được giao cho consumer nhưng chưa ACK |
| Total | Tổng số message Ready và Unacked |
| Consumers | Số consumer đang consume queue |

### Queue `demo.dlq`

Dùng để kiểm tra các message đã bị dead-letter, chẳng hạn message `FAIL` hoặc message `RETRY` sau khi hết số lần thử.

### Kiểm tra consumer

Trong RabbitMQ Management UI, mở queue `demo.queue` và xem số lượng Consumers. Khi Worker kết nối thành công, chỉ số này phải lớn hơn hoặc bằng 1, tùy số listener đang chạy.

## 10. Lỗi thường gặp

### Lỗi không kết nối được RabbitMQ

Kiểm tra:

```bash
docker ps
docker logs rabbitmq-demo
```

Đảm bảo RabbitMQ đang chạy, port `5672` được mở và thông tin đăng nhập đúng.

### Lỗi queue khai báo không tương thích

Nếu queue `demo.queue` đã tồn tại trước khi thêm cấu hình DLX, RabbitMQ có thể báo lỗi `PRECONDITION_FAILED` khi ứng dụng khai báo lại queue với các argument khác.

Trong môi trường demo, nếu chắc chắn không cần giữ message cũ, có thể xóa queue rồi khởi động lại ứng dụng để tạo queue với cấu hình mới.

**Cảnh báo:** Xóa queue có thể làm mất message đang chờ. Không thực hiện trên môi trường có dữ liệu quan trọng.

### Message không vào DLQ

Kiểm tra lần lượt:

1. `demo.queue` có cấu hình dead-letter exchange là `demo.dlx` hay không.
2. `demo.dlx` có binding tới `demo.dlq` bằng routing key `demo.dead` hay không.
3. Worker có reject message mà không requeue hay không.
4. Listener có retry và xử lý exception theo đúng cấu hình mong muốn hay không.

### Message được xử lý ở terminal khác

Nếu cả API và Worker đều có `@RabbitListener` trên cùng queue, RabbitMQ có thể phân phối message cho một trong hai consumer.

Hãy chỉ để Worker consume queue nếu muốn tách riêng nhiệm vụ xử lý.

## 11. Dừng hệ thống

Dừng các ứng dụng Spring Boot bằng `Ctrl + C` trong terminal tương ứng.

Dừng RabbitMQ container:

```bash
docker compose down
```

Lệnh trên giữ lại named volume `rabbitmq_data`.

Nếu muốn xóa cả volume và dữ liệu RabbitMQ của demo:

```bash
docker compose down -v
```

**Cảnh báo:** Lệnh `docker compose down -v` sẽ xóa volume được khai báo trong Compose, bao gồm dữ liệu RabbitMQ được lưu trong volume đó.

## 12. Giới hạn hiện tại

- API trả về kết quả publish, không phải kết quả xử lý cuối cùng của Worker.
- ACK là giao tiếp xác nhận giữa consumer và RabbitMQ, không tự động cập nhật trạng thái trên frontend.
- RabbitMQ Management UI cung cấp chỉ số queue và consumer, nhưng không mặc định biết trạng thái nghiệp vụ của từng message.
- Muốn hiển thị kết quả xử lý từng message trên dashboard, cần bổ sung cơ chế theo dõi trạng thái, chẳng hạn lưu trạng thái vào database hoặc phát sự kiện qua WebSocket.

## 13. Mục tiêu demo

Sau khi chạy thành công, có thể trình bày được:

- Producer gửi message qua RabbitMQ.
- Worker độc lập nhận và xử lý message.
- ACK sau khi xử lý thành công.
- Unacked trong quá trình xử lý chậm.
- Retry khi xảy ra lỗi tạm thời.
- DLX/DLQ khi message bị dead-letter.
- RabbitMQ Management UI dùng để giám sát queue và consumer.
