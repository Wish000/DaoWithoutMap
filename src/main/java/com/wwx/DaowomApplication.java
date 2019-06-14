package com.wwx;

import com.wwx.compiler.DaoFactory;
import com.wwx.dao.Dao;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAutoConfiguration
@EnableAsync
public class DaowomApplication {

	public static void main(String[] args) {
		DaoFactory.createDaoImpls("com.wwx.dao", DaowomApplication.class);
		SpringApplication.run(DaowomApplication.class, args);
	}
}
