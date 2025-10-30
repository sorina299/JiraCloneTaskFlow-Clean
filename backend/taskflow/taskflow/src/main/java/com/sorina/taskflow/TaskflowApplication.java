package com.sorina.taskflow;

import com.sorina.taskflow.entity.Role;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.repository.RoleRepository;
import com.sorina.taskflow.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class TaskflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskflowApplication.class, args);
	}
}
