package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamesFetcherLambda implements RequestHandler<Void, List<Map<String, String>>> {

    @Override
    public List<Map<String, String>> handleRequest(Void input, Context context) {
        List<Map<String, String>> games = new ArrayList<>();
        context.getLogger().log("Start");
        try {
            context.getLogger().log("Pre Secret Manager");
            SecretsManagerClient client = SecretsManagerClient.builder().region(Region.EU_NORTH_1).build();
            context.getLogger().log("Secret Manager");
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId("rds-db-credentials/steamdb/postgres/1747922949063")
                    .build();
            GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
            context.getLogger().log("Response: " + getSecretValueResponse.secretString());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> secretMap = objectMapper.readValue(getSecretValueResponse.secretString(), Map.class);

            String url = String.format("jdbc:postgresql://%s:%s/%s",
                    secretMap.get("host"), secretMap.get("port"), secretMap.get("dbname"));
            context.getLogger().log("PreConnection");
            try (Connection conn = DriverManager.getConnection(url, secretMap.get("username"), secretMap.get("password"));
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM games");
                 ResultSet rs = stmt.executeQuery()) {
                context.getLogger().log("Connection");
                while (rs.next()) {
                    Map<String, String> game = new HashMap<>();
                    game.put("id", rs.getString("id"));
                    game.put("name", rs.getString("name"));
                    game.put("imageUrl", rs.getString("image_url"));
                    games.add(game);
                }
            }

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
        }
        context.getLogger().log("Size: " + games.size());
        return games;
    }
}
