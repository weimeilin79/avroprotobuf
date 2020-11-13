package demo.camel;

import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;



/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    

    public void configure() {
        
//&serializerClass=org.apache.kafka.common.serialization.ByteBufferSerializer


    restConfiguration().port("8081").bindingMode(RestBindingMode.auto);
    rest("/").post("transfer").consumes("application/json").to("direct:transfer");

    from("direct:transfer")   
      .marshal().protobuf("demo.camel.TransactionProtos$Transaction")
      .log("Sender: ${header.sender}")
      .toD("kafka:webtrans-quarkus?brokers=localhost:9092&key=${header.sender}&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer")
      .setBody(simple("Transaction from ${header.sender} sent."));

    }
    

}
