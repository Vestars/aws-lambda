
- Create a query in SQS and name it, thats provide you url to this query topic and later you need it to link your lambda function to receive messages from it.

Instruction for lambda-sqs-sender
1. Create lambda function into AWS (select runtime: java 21 and write handler path: com.example.lambda.SqsSenderLambda::handleRequest )
2. Upload your code to AWS lambda, it should be .jar file after you make package step in maven
3. Add to "Environment variables" queue path (name it SQS_URL and provide your url to created query) ex: https://sqs.REGION.amazonaws.com/UID/QUERY-NAME
4. Also provide permissions for CloudWatchLogs and sqs sendMessage


- Create database in RDS (I used postgreSQL) 

Instruction for lambda-get-games
