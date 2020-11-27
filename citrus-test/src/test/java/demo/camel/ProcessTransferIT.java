/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package demo.camel;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.junit.JUnit4CitrusSupport;
import com.consol.citrus.message.MessageType;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import demo.camel.config.EndpointConfig;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import static com.consol.citrus.actions.CreateVariablesAction.Builder.createVariable;
import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;
import static com.consol.citrus.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static com.consol.citrus.http.actions.HttpActionBuilder.http;

@ContextConfiguration(classes = EndpointConfig.class)
public class ProcessTransferIT extends JUnit4CitrusSupport {

    @Autowired
    private HttpClient transactionClient;

    @Test
    @CitrusTest
    public void shouldTransfer() throws InvalidProtocolBufferException {
        given(createVariable("id", "A0citrus:randomNumber(5)"));
        given(createVariable("userId", "chrissy"));
        given(createVariable("receiverId", "Citrus"));

        // when send transaction
        when(http().client(transactionClient)
                .send()
                .post("/transfer")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(JsonFormat.printer().print(TransactionProtos.Transaction
                        .newBuilder()
                        .setTransactionid("${id}")
                        .setTransactiontype("NORMALADD")
                        .setSender(TransactionProtos.Transaction.User.newBuilder()
                                .setUsername("Christina")
                                .setUserid("${userId}")
                                .build())
                        .setReceiverid("${receiverId}")
                        .setCurrency("USD")
                        .setAmt(100.00D))));

        then(http().client(transactionClient)
                .receive()
                .response(HttpStatus.OK)
                .payload("Transaction from ${userId} sent."));

        // then wait for and verify entries in MongoDb
        String mongoDbTransactionsUri = "camel:sync:mongodb:mongoClient?database=example&collection=transaction";
        then(repeatOnError()
                .condition((index, context) -> index > 10)
                .actions(
                    send(mongoDbTransactionsUri)
                        .header(MongoDbConstants.OPERATION_HEADER, "count")
                        .payload("{\"transactionid\": \"${id}\"}"),
                    receive(mongoDbTransactionsUri)
                            .messageType(MessageType.PLAINTEXT)
                            .payload("2")
                )
        );

        // then verify transaction sender
        then(send(mongoDbTransactionsUri)
                .header(MongoDbConstants.OPERATION_HEADER, "findOneByQuery")
                .payload("{" +
                    "\"transactionid\": \"${id}\"," +
                    "\"userid\": \"${userId}\"" +
                "}")
        );

        then(receive(mongoDbTransactionsUri)
                .validationCallback((message, context) -> {
                    Document sender = message.getPayload(Document.class);
                    Assertions.assertThat(sender.get("transactiontype")).isEqualTo("SUB");
                    Assertions.assertThat(sender.get("amt")).isEqualTo("100.0");
                    Assertions.assertThat(sender.get("currency")).isEqualTo("USD");
                })
        );

        // then verify transaction receiver
        then(send(mongoDbTransactionsUri)
                .header(MongoDbConstants.OPERATION_HEADER, "findOneByQuery")
                .payload("{" +
                    "\"transactionid\": \"${id}\"," +
                    "\"userid\": \"${receiverId}\"" +
                "}")
        );

        then(receive(mongoDbTransactionsUri)
                .validationCallback((message, context) -> {
                    Document receiver = message.getPayload(Document.class);
                    Assertions.assertThat(receiver.get("transactiontype")).isEqualTo("ADD");
                    Assertions.assertThat(receiver.get("amt")).isEqualTo("100.0");
                    Assertions.assertThat(receiver.get("currency")).isEqualTo("USD");
                })
        );
    }
}
