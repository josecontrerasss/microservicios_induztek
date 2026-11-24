package cl.induztek.protocolos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ProtocolosApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProtocolosApplication.class, args);
	}

}
