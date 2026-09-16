package com.kopa.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableMongoRepositories(basePackages = "com.kopa.repository")
@EnableMongoAuditing
public class DbConfig {

    private static final Logger log = LoggerFactory.getLogger(DbConfig.class);

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    static {
        // Automatically inject .env variables into System properties
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }

    public String getMongoUri() {
        String uri = System.getProperty("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            uri = System.getenv("MONGODB_URI");
        }
        if (uri == null || uri.isBlank()) {
            uri = dotenv.get("MONGODB_URI");
        }
        if (uri == null || uri.isBlank()) {
            uri = dotenv.get("SPRING_DATA_MONGODB_URI");
        }
        if (uri == null || uri.isBlank()) {
            uri = "mongodb+srv://<db_username>:<db_password>@kopa.tdeuo4k.mongodb.net/?appName=kopa";
        }
        return uri.trim();
    }

    public String getDatabaseName() {
        String dbName = System.getProperty("SPRING_DATA_MONGODB_DATABASE");
        if (dbName == null || dbName.isBlank()) {
            dbName = dotenv.get("SPRING_DATA_MONGODB_DATABASE");
        }
        if (dbName == null || dbName.isBlank()) {
            dbName = "kopa";
        }
        return dbName.trim();
    }

    @Bean
    @Primary
    public MongoClient mongoClient() {
        String uri = getMongoUri();

        if (uri.contains("<db_username>") || uri.contains("<db_password>")) {
            log.warn("⚠️ NOTICE: MONGODB_URI contains placeholder credentials <db_username>:<db_password>. " +
                     "Please update your .env file with your actual MongoDB Atlas username and password.");
        }

        try {
            ConnectionString connectionString = new ConnectionString(uri);
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToSocketSettings(builder ->
                    builder.connectTimeout(5, TimeUnit.SECONDS)
                           .readTimeout(5, TimeUnit.SECONDS))
                .build();
            log.info("Configured MongoDB client for cluster: {}", connectionString.getHosts());
            return MongoClients.create(settings);
        } catch (Exception e) {
            log.warn("Could not parse MongoDB connection URI ({}). Falling back to default client: {}", uri, e.getMessage());
            return MongoClients.create();
        }
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        String dbName = getDatabaseName();
        log.info("Initialized MongoTemplate with database: '{}'", dbName);
        return new MongoTemplate(mongoClient, dbName);
    }
}
