package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.avro.AvroDataFormat;
/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    

    public void configure() {
        AvroDataFormat format = new AvroDataFormat(Transaction.SCHEMA$);

        from("kafka:transrec-quarkus?brokers=localhost:9092&valueDeserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer")
        .unmarshal(format)
        .log("---->originalmsg ${body}")
        .convertBodyTo(String.class)
        .to("mongodb:mymongo?database=example&collection=transaction&operation=save")
        ;

    }
    

}
