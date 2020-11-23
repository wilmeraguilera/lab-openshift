package org.company.example.resource;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;

import org.company.example.entity.ResponseHealthCheck;
import org.company.example.exception.UserNotFoundException;
import org.company.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.company.example.entity.User;


@RestController
public class UserResource {
	
	private Logger logger = LoggerFactory.getLogger(UserResource.class);
	
	@Autowired
	private UserRepository userRepository;
	
	@Value("${app.title}")
	private String appTitle;
	
	@GetMapping("/healthcheck")
	public ResponseEntity<ResponseHealthCheck> healthcheck() throws UnknownHostException {
		String ip = InetAddress.getLocalHost().getHostAddress();
		ResponseHealthCheck responseHealthCheck = new ResponseHealthCheck();
		responseHealthCheck.setAppTitle(appTitle);
		responseHealthCheck.setIP(ip);
		responseHealthCheck.setVersion("1.0");
		return new ResponseEntity<ResponseHealthCheck>(responseHealthCheck, HttpStatus.OK);
	}

	@GetMapping("/users")
	public List<User> retrieveAllUsers() {
		logger.info("Consultar usuario");
		List<User> users = userRepository.findAll();
		
		for(User user: users) {
			logger.info(user.toString());
		}
		
		return users;
	}
	

	@GetMapping("/usersAll")
	public List<User> retrieveAll() {
		logger.info("COnsultar usuario");
		List<User> users = userRepository.findAll();
		for(User user: users) {
			logger.info(user.toString());
		}
		return users;
	}

	@GetMapping("/users/{id}")
	public User retrieveUser(@PathVariable long id) {
		Optional<User> student = userRepository.findById(id);

		if (!student.isPresent())
			throw new UserNotFoundException("id-" + id);

		return student.get();
	}

	@DeleteMapping("/users/{id}")
	public void deleteUser(@PathVariable long id) {
		userRepository.deleteById(id);
	}

	@PostMapping("/users")
	public ResponseEntity<Object> createUser(@RequestBody User user) {
		User savedUser = userRepository.save(user);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(savedUser.getId()).toUri();

		return ResponseEntity.created(location).build();

	}
	
	@PutMapping("/users/{id}")
	public ResponseEntity<Object> updateUser(@RequestBody User user, @PathVariable long id) {

		Optional<User> userOptional = userRepository.findById(id);

		if (!userOptional.isPresent())
			return ResponseEntity.notFound().build();

		user.setId(id);
		userRepository.save(user);

		return ResponseEntity.noContent().build();
	}

}
