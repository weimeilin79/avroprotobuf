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
        String registryconfigAvro="?additionalProperties.apicurio.registry.url={{registryurl}}";
        registryconfigAvro += "&additionalProperties.apicurio.registry.avro-datum-provider={{datumprovider}}";
        registryconfigAvro += "&additionalProperties.apicurio.registry.global-id={{globalid}}";
        //AvroDataFormat format = new AvroDataFormat(Transaction.SCHEMA$); //We don't need to do it because the serializer in kafka config does it
        from("kafka:transrec"+registryconfigAvro)
            .log("---->originalmsg ${body} ")
            //.unmarshal(format) //We don't need to do it because the serializer in kafka config does it
            //.convertBodyTo(String.class)
            .to("mongodb:mymongo?database=example&collection=transaction&operation=save")
        ;
        
    }

    
    

    
}
