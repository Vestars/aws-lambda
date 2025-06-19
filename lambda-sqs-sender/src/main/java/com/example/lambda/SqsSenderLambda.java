package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.model.Game;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SqsSenderLambda implements RequestHandler<Game, Void> {

    private final SqsClient sqsClient = SqsClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Void handleRequest(Game game, Context context) {
        String queueUrl = System.getenv("SQS_URL");

        try {
            String messageBody = objectMapper.writeValueAsString(game);
            SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build();

            sqsClient.sendMessage(request);
            context.getLogger().log("Message sent: " + messageBody);
        } catch (Exception e) {
            context.getLogger().log("Error sending message: " + e.getMessage());
        }

        return null;
    }
}
