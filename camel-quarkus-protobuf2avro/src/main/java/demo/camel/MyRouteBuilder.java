package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import java.io.InputStream;
import org.apache.camel.dataformat.avro.AvroDataFormat;
/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    

    public void configure() {
        AvroDataFormat format = new AvroDataFormat(Transaction.SCHEMA$);
//

        from("kafka:webtrans-quarkus?brokers=localhost:9092&groupId=consumergroup&valueDeserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer")
            .bean(demo.camel.TransactionProtos.Transaction.class, "parseFrom(${body})")
            .log("transactionid --> [${body.transactionid}]")
            .setHeader("originalmsg",simple("${body}"))
            .multicast().parallelProcessing()
            .to("direct:sender","direct:receiver") 
        ;

        from("direct:sender")
            .setBody(method(this,"setValue(${header.originalmsg.sender.userid}, ${header.originalmsg.transactionid}, \"SUB\",${header.originalmsg.currency},${header.originalmsg.amt} )"))
            .marshal(format) //We don't need to do it because the serializer in kafka config does it
            .log("sender class --> [${body.class}]")
            .log("sender body --> [${body}]")
            .toD("kafka:transrec-quarkus?brokers=localhost:9092&groupId=producergroup&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer")
        ;
        //&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer
        
        from("direct:receiver")
            .setBody(method(this,"setValue(${header.originalmsg.receiverid}, ${header.originalmsg.transactionid}, \"ADD\", ${header.originalmsg.currency}, ${header.originalmsg.amt} )"))
            .marshal(format) //We don't need to do it because the serializer in kafka config does it
            .log("sender class --> [${body.class}]")
            .log("receiver body --> [${body}]")
            .toD("kafka:transrec-quarkus?brokers=localhost:9092&groupId=producergroup&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer")
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
