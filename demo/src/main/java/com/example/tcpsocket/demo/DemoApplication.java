package com.example.tcpsocket.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

@SpringBootApplication
@EnableIntegration
@IntegrationComponentScan
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Value("15000")
    private int serverPort;

    @Value("5000")
    private int serverTimeout;

    @Bean
    public TcpNetClientConnectionFactory clientConnectionFactory() {
        TcpNetClientConnectionFactory cf = new TcpNetClientConnectionFactory("172.16.10.131", serverPort);
        cf.setSerializer(new ByteArrayCrLfSerializer());
        cf.setDeserializer(new ByteArrayCrLfSerializer());
        cf.setSoTimeout(serverTimeout);
        return cf;
    }

    @MessagingGateway(defaultRequestChannel = "outboundGatewayChannel")
    public interface TcpClientGateway {
        String send(String data);
    }

    @Bean
    public MessageChannel outboundGatewayChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "outboundGatewayChannel")
    public TcpOutboundGateway outboundGateway(TcpNetClientConnectionFactory clientConnectionFactory) {
        TcpOutboundGateway gateway = new TcpOutboundGateway();
        gateway.setConnectionFactory(clientConnectionFactory);
        gateway.setRequestTimeout(serverTimeout);
        gateway.setReplyChannelName("clientOutputChannel");
        return gateway;
    }

    @Bean
    public CommandLineRunner sendData(TcpClientGateway gateway) {
        return args -> {
            String response = gateway.send("Hello World");
            System.out.println("Received response from server: " + response);
        };
    }

	@Bean
	public MessageChannel clientOutputChannel() {
		return new DirectChannel();
	}

	@Bean
	@ServiceActivator(inputChannel = "clientOutputChannel")
	public MessageHandler clientMessageHandler() {
		return message -> {
			String response = new String((byte[]) message.getPayload());
			System.out.println("Message from server: " + response);
		};
	}
}
