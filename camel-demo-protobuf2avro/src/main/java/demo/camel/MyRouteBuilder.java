package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.avro.AvroDataFormat;
/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    
    
    public void configure() {
        AvroDataFormat format = new AvroDataFormat(Transaction.SCHEMA$);
        String additionalinfo="&additionalProperties.apicurio.registry.url=http://example-apicurioregistry.test.apps.cluster-b2f1.b2f1.example.opentlc.com/api";
        additionalinfo = additionalinfo+"&additionalProperties.apicurio.registry.artifact-id=io.apicurio.registry.utils.serde.strategy.SimpleTopicIdStrategy";
        additionalinfo = additionalinfo+"&additionalProperties.apicurio.registry.global-id=io.apicurio.registry.utils.serde.strategy.GetOrCreateIdStrategy";
        additionalinfo = additionalinfo+"&additionalProperties.apicurio.registry.avro-datum-provider=io.apicurio.registry.utils.serde.avro.ReflectAvroDatumProvider";
        additionalinfo = additionalinfo+"&serializerClass=io.apicurio.registry.utils.serde.AvroKafkaSerializer";
       
        String additionalinfo2="&additionalProperties.apicurio.registry.url=http://example-apicurioregistry.test.apps.cluster-b2f1.b2f1.example.opentlc.com/api";
        additionalinfo2 = additionalinfo2+"&additionalProperties.apicurio.registry.avro-datum-provider=io.apicurio.registry.utils.serde.avro.ReflectAvroDatumProvider";
        additionalinfo2 = additionalinfo2+"&valueDeserializer=io.apicurio.registry.utils.serde.AvroKafkaDeserializer";
        
        from("kafka:weborder?brokers=localhost:9092&groupId=consumergroup&valueDeserializer=org.apache.kafka.common.serialization.ByteBufferDeserializer")
            .unmarshal().protobuf("demo.camel.TransactionProtos$Transaction")
            .setHeader("originalmsg",simple("${body}"))
            .log("---->originalmsg ${headers.originalmsg}")
            .multicast().parallelProcessing()
            .to("direct:sender","direct:receiver") 
        ;

        from("direct:sender")
            .setBody(method(this,"setValue(   ${header.originalmsg.sender.userid}, ${header.originalmsg.transactionid}, \"SUB\",${header.originalmsg.currency},${header.originalmsg.amt} )"))
            .marshal(format)
            .toD("kafka:demo-avro?brokers=localhost:9092&groupId=mygroup&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer")
            //+additionalinfo
        ;
        //&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer
        
        from("direct:receiver")
            .setBody(method(this,"setValue( ${header.originalmsg.receiverid}, ${header.originalmsg.transactionid}, \"ADD\", ${header.originalmsg.currency}, ${header.originalmsg.amt} )"))
            .marshal(format)
            .toD("kafka:demo-avro?brokers=localhost:9092&groupId=mygroup&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer")
             //+additionalinfo
        ;
        //&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer
        from("kafka:demo-avro?brokers=localhost:9092&groupId=mygroup")
            .unmarshal(format)
            .log("---->avro result ${body.class} ${body} ${headers}")
        ;
        //&valueDeserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer
    }

    
    public Transaction setValue(String userid, 
                                String transactionid, String transactiontype,
                                String currency, String amt ){
        
        Transaction transaction = new Transaction();

        transaction.setUserid(userid);
        transaction.setTransactionid(transactionid);
        transaction.setTransactiontype(transactiontype);
        transaction.setCurrency(currency);
        transaction.setAmt(amt);

        return transaction;
       
    }

    
}
