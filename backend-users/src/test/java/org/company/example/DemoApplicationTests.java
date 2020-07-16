package org.company.example;


import org.company.example.entity.ResponseHealthCheck;
import org.company.example.entity.User;
import org.company.example.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import org.company.example.resource.UserResource;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoApplicationTests {
	
	@Autowired
	UserResource userResource;

	@MockBean
	UserRepository userRepository;

	public DemoApplicationTests() {
		super();

	}

	@Test
	public void healthCheck() throws UnknownHostException {
		 ResponseEntity<ResponseHealthCheck> response = userResource.healthcheck();
		 Assert.assertNotNull(response.getBody().getIP());
	}

	@Test
	public void getUsersTest() throws UnknownHostException {
		mocksRepository();
		List<User> listaUsers = userResource.retrieveAllUsers();
		Assert.assertEquals(1, listaUsers.size());
	}

	private void mocksRepository() {
		User user1 = new User();
		user1.setEmail("testa@test.com");
		user1.setId(new Long(1));
		user1.setLastName("Perez");
		user1.setName("Fabio");
		List<User> mocklistUsers = new ArrayList<>();
		mocklistUsers.add(user1);
		Mockito.when(userRepository.findAll()).thenReturn(mocklistUsers);
	}

}
