package demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.avro.AvroDataFormat;
/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    
    
    public void configure() {
        //AvroDataFormat format = new AvroDataFormat(Transaction.SCHEMA$); //We don't need to do it because the serializer in kafka config does it
        String registryconfig="&additionalProperties.apicurio.registry.url={{registryurl}}";
        String registryconfigAvro = registryconfig+"&additionalProperties.apicurio.registry.avro-datum-provider={{datumprovider}}";
        registryconfigAvro +="&additionalProperties.apicurio.registry.global-id={{globalid}}";
        
        
        from("kafka:webtrans?groupId=consumergroup&valueDeserializer={{deserializerClass}}"+registryconfig)
            .marshal().protobuf("demo.camel.TransactionProtos$Transaction")
            .bean(demo.camel.TransactionProtos.Transaction.class, "parseFrom(${body})")
            .setHeader("originalmsg",simple("${body}"))
            .multicast().parallelProcessing()
            .to("direct:sender","direct:receiver") 
        ;

        from("direct:sender")
            .setBody(method(this,"setValue(   ${header.originalmsg.sender.userid}, ${header.originalmsg.transactionid}, \"SUB\",${header.originalmsg.currency},${header.originalmsg.amt} )"))
            //.marshal(format) //We don't need to do it because the serializer in kafka config does it
            .toD("kafka:transrec?serializerClass={{serializerClass}}"+registryconfigAvro)
        ;
        //&serializerClass=org.apache.kafka.common.serialization.ByteArraySerializer
        
        from("direct:receiver")
            .setBody(method(this,"setValue( ${header.originalmsg.receiverid}, ${header.originalmsg.transactionid}, \"ADD\", ${header.originalmsg.currency}, ${header.originalmsg.amt} )"))
            //.marshal(format) //We don't need to do it because the serializer in kafka config does it
            .toD("kafka:transrec?serializerClass={{serializerClass}}"+registryconfigAvro)
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
