package com.vatika.workorder;

import org.springframework.boot.SpringApplication;

public class TestWorkOrderPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(WorkOrderPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
