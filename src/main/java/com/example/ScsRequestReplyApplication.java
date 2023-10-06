package com.example;

import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.HeaderEnricherSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Payload;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class ScsRequestReplyApplication {

	public static void main(String[] args) throws InterruptedException, StreamReadException, DatabindException, IOException {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(ScsRequestReplyApplication.class,
				args);
		QueueGateway gateway = applicationContext.getBean(QueueGateway.class);
		ObjectMapper objectMapper = new ObjectMapper();
		Thread.sleep(5000);

		while (true) {
			var ping = Ping.builder().cid("123456").currentTimeMillis(currentTimeMillis()).build();
//			var pong = gateway.apply(ping);
			var byteArray = gateway.handle(ping);
			var pong = objectMapper.readValue(byteArray, Pong.class);

			System.out.println(pong);
			Thread.sleep(1000);
		}
	}
	
	@Bean
	public MessageChannel request() {
		return MessageChannels.direct().get();
	}

	@Bean
	public MessageChannel reply() {
		return MessageChannels.direct().get();
	}
	
	public static final String HANDLER_FLOW = "handlerFlow";
	
	@Bean
	public IntegrationFlow requestsFlow() {
		return IntegrationFlow.from(HANDLER_FLOW).enrichHeaders(HeaderEnricherSpec::headerChannelsToString)
				.transform(new ObjectToJsonTransformer())
				.channel("request").get();
	}
	
	@MessagingGateway
	public interface QueueGateway {
		@Gateway(requestChannel = HANDLER_FLOW, replyChannel = "reply")
		byte[] handle(@Payload Ping payload);
	}
//	
//	@MessagingGateway(defaultRequestChannel = "handleRequest")
//	public interface QueueGateway
//			extends Function<Ping, Pong> { }
	
    @Bean
	public Function<Ping, Pong> requestReply() {
		return msg -> {
			System.out.println("Consumer %s".formatted(msg));
			return Pong.builder().responseCid(msg.getCid())
					.duration(currentTimeMillis() - msg.getCurrentTimeMillis()).build();
		};
    }

}
