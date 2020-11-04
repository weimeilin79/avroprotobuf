package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.model.rest.RestBindingMode;


/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    
    public void configure() {

        String registryconfig="&additionalProperties.apicurio.registry.url={{registryurl}}";
        registryconfig +="&additionalProperties.apicurio.registry.global-id={{globalid}}";
        //ProtobufDataFormat format = new ProtobufDataFormat("demo.camel.TransactionProtos$Transaction");

        restConfiguration().component("netty-http").host("localhost").port("8081").bindingMode(RestBindingMode.auto);
        
        rest("/").post("transfer").consumes("application/json").to("direct:transfer");

        from("direct:transfer")
        
        .setHeader("sender",jsonpath("$.sender.userid"))
        .marshal().protobuf("demo.camel.TransactionProtos$Transaction")
        .bean(demo.camel.TransactionProtos.Transaction.class, "parseFrom(${body})")
        .toD("kafka:webtrans?key=${header.sender}"+registryconfig)
        .setBody(simple("Transaction from ${header.sender} sent."))
        ;

    }
    
    
    

    
}
