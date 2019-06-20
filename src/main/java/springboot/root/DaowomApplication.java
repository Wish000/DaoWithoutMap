package springboot.root;

import com.wwx.compiler.DaoFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAutoConfiguration
@EnableAsync
public class DaowomApplication {

	public static void main(String[] args) {
		DaoFactory.createDaoImpls("springboot.root.dao", DaowomApplication.class);
		SpringApplication.run(DaowomApplication.class, args);
	}
}
