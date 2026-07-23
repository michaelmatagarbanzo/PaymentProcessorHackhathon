package com.ecommerce.sale.infrastructure.config;

import com.mongodb.client.MongoCollection;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    private static final String SALE_COLLECTION = "Transactions";

    @Bean
    @Profile("!local")
    ApplicationRunner mongoIndexInitializer(MongoTemplate mongoTemplate) {
        return args -> {
            MongoCollection<Document> collection = mongoTemplate.getCollection(SALE_COLLECTION);

            collection.createIndex(
                new Document("transactionId", 1),
                new com.mongodb.client.model.IndexOptions()
                    .name("idx_transactionId_unique")
                    .unique(true)
            );

            collection.createIndex(
                new Document("createdAt", 1),
                new com.mongodb.client.model.IndexOptions()
                    .name("idx_createdAt_ttl")
                    .expireAfter(548L, TimeUnit.DAYS)
            );

            collection.createIndex(
                new Document("terminalId", 1)
                    .append("invoice", 1)
                    .append("totalAmount", 1)
                    .append("accountNumber", 1)
                    .append("transactionType", 1),
                new com.mongodb.client.model.IndexOptions().name("idx_duplicate_validation")
            );
        };
    }
}
