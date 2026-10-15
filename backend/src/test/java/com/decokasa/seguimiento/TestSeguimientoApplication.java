package com.decokasa.seguimiento;

import org.springframework.boot.SpringApplication;

public class TestSeguimientoApplication {

	public static void main(String[] args) {
		SpringApplication.from(SeguimientoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
