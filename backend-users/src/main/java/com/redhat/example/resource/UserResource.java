package com.redhat.example.resource;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.redhat.example.entity.User;
import com.redhat.example.exception.UserNotFoundException;
import com.redhat.example.repository.UserRepository;

@RestController
public class UserResource {
	
	@Autowired
	private UserRepository userRepository;
	
	@Value("${app.title}")
	private String appTitle;
	
	@GetMapping("/healthcheck")
	public String healthcheck() {
		return appTitle + " - Status UP Modified!";
	}

	@GetMapping("/users")
	public List<User> retrieveAllUsers() {
		return userRepository.findAll();
	}
	

	@GetMapping("/usersAll")
	public List<User> retrieveAll() {
		return userRepository.findAll();
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
