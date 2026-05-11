package com.ops.twin;

import com.ops.twin.config.TestSecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql(scripts = {"/schema.sql", "/test-data.sql"},
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Import(TestSecurityConfig.class)
public abstract class BaseTest {
}
