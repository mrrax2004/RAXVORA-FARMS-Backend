package com.dairyfarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dairyfarm.model.UserRole;
import com.dairyfarm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "spring.profiles.active=demo",
        "spring.datasource.url=jdbc:h2:mem:dfms-demo-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "ADMIN_USERNAME=admin",
        "ADMIN_PASSWORD=admin",
        "app.upload-dir=target/demo-test-uploads"
})
class DemoAdminProvisioningTest {
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void provisionsDemoAdminWithEncodedPasswordAndAdminRole() {
        var admin = users.findByUsername("admin").orElseThrow();

        assertTrue(passwordEncoder.matches("admin", admin.getPassword()));
        assertEquals(UserRole.ADMIN, admin.getRoles().iterator().next().getName());
    }
}
