package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.model.rest.RestBindingMode;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    
    public void configure() {
        restConfiguration().component("netty-http").host("localhost").port("8080").bindingMode(RestBindingMode.auto);
        
        rest("/").post("transfer").consumes("application/json").to("direct:transfer");

        from("direct:transfer")
       
        .setHeader("receiverid",jsonpath("$.receiverid"))
        .setHeader("sender",jsonpath("$.sender.userid"))
        .log("---->json ${body.class}")
        .log("---->header ${headers}")
        .marshal().protobuf("demo.camel.TransactionProtos$Transaction")
        .toD("kafka:weborder?brokers=localhost:9092&groupId=producergroup&serializerClass=org.apache.kafka.common.serialization.ByteBufferSerializer&key=${header.sender}")
        
        ;

    }


    
}
