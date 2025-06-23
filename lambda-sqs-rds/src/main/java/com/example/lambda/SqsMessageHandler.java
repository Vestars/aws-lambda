package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;

public class SqsMessageHandler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        if (event.getRecords().isEmpty()){
            return null;
        }

        try {
            Map<String, String> secretMap = retrieveDatabaseCredentials();
            String url = databaseUrl(secretMap);
            try (Connection connection = DriverManager.getConnection(url, secretMap.get("username"), secretMap.get("password"))) {
                for (SQSEvent.SQSMessage message : event.getRecords()) {
                    String body = message.getBody();
                    Game game = objectMapper.readValue(body, Game.class);

                    context.getLogger().log("Inserting to DB: " + game);
                    insertIntoDatabase(connection, game.name(), game.imageUrl());
                }
            }


        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
        }
        return null;
    }

    private void insertIntoDatabase(Connection connection, String name, String imageUrl) throws Exception {
        String sql = "INSERT INTO games (name, image_url) VALUES (?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, name);
        stmt.setString(2, imageUrl);
        stmt.executeUpdate();
        stmt.close();
    }

    private Map<String, String> retrieveDatabaseCredentials() throws Exception {
        SecretsManagerClient client = SecretsManagerClient.builder().region(Region.EU_NORTH_1).build();
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId("rds-db-credentials/steamdb/postgres/1747922949063")
                .build();
        GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        return objectMapper.readValue(getSecretValueResponse.secretString(), Map.class);
    }

    private String databaseUrl(Map<String, String> connectionData) {
        return String.format("jdbc:postgresql://%s:%s/%s",
                connectionData.get("host"), connectionData.get("port"), connectionData.get("dbname"));
    }

    public record Game(String name, String imageUrl) {}
}
