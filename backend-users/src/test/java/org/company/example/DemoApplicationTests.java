package org.company.example;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import org.company.example.resource.UserResource;

import java.net.UnknownHostException;

@RunWith(SpringRunner.class)
@SpringBootTest
class DemoApplicationTests {
	
	@Autowired
	UserResource userResource;

	@Test
	void contextLoads() {
		
	}
	
	
	@Test
	void healthCheck() throws UnknownHostException {
		userResource.healthcheck();
	}

}