package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.avro.AvroDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.component.jackson.JacksonDataFormat;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import org.apache.camel.BindToRegistry;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {
    @BindToRegistry("mymongo")
    MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        
    public void configure() {
        AvroDataFormat format = new AvroDataFormat(Transaction.SCHEMA$);
         
        from("kafka:transrec?additionalProperties.apicurio.registry.url={{registryurl}}&additionalProperties.apicurio.registry.avro-datum-provider={{datumprovider}}")
            .unmarshal(format)
            .log("---->originalmsg ${body} ")
            .convertBodyTo(String.class)
            .to("mongodb:mymongo?database=example&collection=transaction&operation=save")
        ;
        
    }

    
    

    
}
