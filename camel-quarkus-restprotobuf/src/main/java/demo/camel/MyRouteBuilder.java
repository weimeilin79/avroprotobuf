package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import java.io.InputStream;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    

    public void configure() {
        
//&serializerClass=org.apache.kafka.common.serialization.ByteBufferSerializer

        from("netty-http:http://0.0.0.0:8081/transfer?CamelHttpMethod=POST&Content-Type=application/json")   
        .setHeader("sender",jsonpath("$.sender.userid"))
        .marshal().protobuf("demo.camel.TransactionProtos$Transaction")
        //.bean(demo.camel.TransactionProtos.Transaction.class, "parseFrom(${body})")
        .log("Sender: ${header.sender}")
        //.log("Sender: ${body.transactionid}")
        .toD("kafka:webtrans-quarkus?brokers=localhost:9092&key=${header.sender}&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer")
        .setBody(simple("Transaction from ${header.sender} sent."))
        ;

    }
    

}
