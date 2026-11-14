package ma.qlinkr;

import org.springframework.boot.SpringApplication;

public class TestQlinkrBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(QlinkrBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
