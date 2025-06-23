package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.model.Game;
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
import java.util.List;
import java.util.Map;

public class GamesFetcherLambda implements RequestHandler<Void, List<Game>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Game> handleRequest(Void input, Context context) {
        List<Game> games = new ArrayList<>();
        try {
            Map<String, String> secretMap = retrieveDatabaseCredentials();
            String url = databaseUrl(secretMap);
            try (Connection connection = DriverManager.getConnection(url, secretMap.get("username"), secretMap.get("password"));
                 PreparedStatement stmt = connection.prepareStatement("SELECT * FROM games")) {
                getDataFromDatabase(stmt, games);
            }
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
        }
        context.getLogger().log("Size: " + games.size());
        return games;
    }

    private void getDataFromDatabase(PreparedStatement stmt, List<Game> games) throws Exception {
        ResultSet resultSet = stmt.executeQuery();
        while (resultSet.next()) {
            Game game = new Game();
            game.setId(resultSet.getInt("id"));
            game.setName(resultSet.getString("name"));
            game.setImageUrl(resultSet.getString("image_url"));
            games.add(game);
        }
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
}
