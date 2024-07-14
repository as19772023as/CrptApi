package ru.strebkov;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.semaphore = new Semaphore(requestLimit);

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> {
            try {
                semaphore.release(requestLimit - semaphore.availablePermits());
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }, 0, timeUnit.toSeconds(1), TimeUnit.SECONDS);
    }

    public void createDocument(String apiUrl, Document document, String signature) {
        try {
            if (!semaphore.tryAcquire()) {
                System.out.println("Лимит запросов - превышен");
                return;
            }

            String requestBody = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Документ успешно создан");
            } else {
                System.out.println("Документ не создан. Ошибка, HTTP-статус: " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("Запрос не отпрален, ошибка: " + e.getMessage());
        } finally {
            semaphore.release();
        }

    }


    public class Document {

        private final Description description;
        private final String doc_id;
        private final String doc_status;
        private final String doc_type;
        private final boolean importRequest;
        private final String owner_inn;
        private final String participant_inn;
        private final String producer_inn;
        private final String production_date;
        private final String production_type;
        private final List<Product> products;
        private final String reg_date;
        private final String reg_number;

        public Document() {
            description = null;
            doc_id = null;
            doc_status = null;
            doc_type = null;
            importRequest = false;
            owner_inn = null;
            participant_inn = null;
            producer_inn = null;
            production_date = null;
            production_type = null;
            products = null;
            reg_date = null;
            reg_number = null;
        }

        public Document(Description description, String doc_id, String doc_status, String doc_type,
                        boolean importRequest, String owner_inn, String participant_inn, String producer_inn,
                        String production_date, String production_type, List<Product> products, String reg_date,
                        String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

        public class Description {
            private String participantInn;
        }

        public class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 7);
        String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        Document document = crptApi.new Document();
        String signature = "I_want_to_work_for_you";

        crptApi.createDocument(apiUrl, document, signature);
    }
}
