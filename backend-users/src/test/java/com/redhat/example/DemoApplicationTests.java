package com.redhat.example;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.redhat.example.repository.UserRepository;
import com.redhat.example.resource.UserResource;

@RunWith(SpringRunner.class)
@SpringBootTest
class DemoApplicationTests {
	
	@Autowired
	UserResource userResource;

	@Test
	void contextLoads() {
		
	}
	
	
	@Test
	void healthCheck() {
		userResource.healthcheck();
	}

}
