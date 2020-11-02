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
        String additionalinfo2="&additionalProperties.apicurio.registry.url=http://example-apicurioregistry.test.apps.cluster-b2f1.b2f1.example.opentlc.com/api";
        additionalinfo2 = additionalinfo2+"&additionalProperties.apicurio.registry.avro-datum-provider=io.apicurio.registry.utils.serde.avro.ReflectAvroDatumProvider";
        additionalinfo2 = additionalinfo2+"&valueDeserializer=io.apicurio.registry.utils.serde.AvroKafkaDeserializer";
        
        //&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer
        from("kafka:demo-avro?brokers=localhost:9092&groupId=mygroup&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer")
            .unmarshal(format)
            .log("---->originalmsg ${body}")
            .convertBodyTo(String.class)
            .to("mongodb:mymongo?database=example&collection=transaction&operation=save")
        ;
        //&valueDeserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer
        //.to("atlas:avroTojson.adm")
    }

    
    

    
}
