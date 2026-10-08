package com.example.financas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DbCheckTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testDb() {
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT email, senha, LENGTH(senha) as len FROM usuario");
        for (Map<String, Object> user : users) {
            System.out.println("USER_ROW: " + user);
        }
    }
}
