package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.avro.AvroDataFormat;
/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    
    
    public void configure() {
        AvroDataFormat format = new AvroDataFormat(Transaction.SCHEMA$);
        String registryconfig="additionalProperties.apicurio.registry.url={{registryurl}}";
        registryconfig +="&additionalProperties.apicurio.registry.global-id={{datumprovider}}";
        registryconfig +="&additionalProperties.apicurio.registry.avro-datum-provider={{globalid}}";
        
        
        from("kafka:webtrans?groupId=consumergroup&valueDeserializer=org.apache.kafka.common.serialization.ByteBufferDeserializer")
            .unmarshal().protobuf("demo.camel.TransactionProtos$Transaction")
            .setHeader("originalmsg",simple("${body}"))
            .multicast().parallelProcessing()
            .to("direct:sender","direct:receiver") 
        ;

        from("direct:sender")
            .setBody(method(this,"setValue(   ${header.originalmsg.sender.userid}, ${header.originalmsg.transactionid}, \"SUB\",${header.originalmsg.currency},${header.originalmsg.amt} )"))
            .marshal(format)
            .toD("kafka:transrec?brokers=localhost:9092&groupId=mygroup"+registryconfig)
        ;
        //&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer
        
        from("direct:receiver")
            .setBody(method(this,"setValue( ${header.originalmsg.receiverid}, ${header.originalmsg.transactionid}, \"ADD\", ${header.originalmsg.currency}, ${header.originalmsg.amt} )"))
            .marshal(format)
            .toD("kafka:transrec?brokers=localhost:9092&groupId=mygroup"+registryconfig)
        ;
        
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
