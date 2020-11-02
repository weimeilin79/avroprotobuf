package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import java.util.LinkedHashMap;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    

    public void configure() {
        //restConfiguration().component("netty-http").host("localhost").port("8080").bindingMode(RestBindingMode.auto);
        
        //rest("/").post("transfer").consumes("application/json").to("direct:transfer");
        
        from("platform-http:/transfer?httpMethodRestrict=POST&consumes=application/json")
        
                .setHeader("receiverid", jsonpath("$.receiverid"))
                .setHeader("sender", jsonpath("$.sender.userid"))
                .convertBodyTo(String.class)
                .marshal().json(JsonLibrary.Jackson,java.util.LinkedHashMap.class)
                .log("---->json ${body}")
                .log("---->header ${headers}")
                .marshal().protobuf("demo.camel.TransactionProtos$Transaction")
                .toD("kafka:weborder?brokers=localhost:9092&groupId=producergroup&serializerClass=org.apache.kafka.common.serialization.ByteBufferSerializer&key=${header.sender}")

        ;

    }
    

}
