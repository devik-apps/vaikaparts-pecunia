package com.devikapps.vaikaparts.conf;

import static com.devikapps.vaikaparts.conf.db.PersistenceConfFactory.create;
import static java.lang.Runtime.getRuntime;

import com.devikapps.vaikaparts.InfraGenerated;
import com.devikapps.vaikaparts.conf.db.PersistenceConf;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Slf4j
@InfraGenerated
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
public abstract class FacadeIT {

  private static final PersistenceConf DB_CONF = create();
  private static final RabbitMQConf RABBITMQ_CONF = new RabbitMQConf();
  private static final EmailConf EMAIL_CONF = new EmailConf();

  @BeforeAll
  static void beforeAll() {
    DB_CONF.start();
    RABBITMQ_CONF.start();
    EMAIL_CONF.start();

    getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  DB_CONF.stop();
                  RABBITMQ_CONF.stop();
                  EMAIL_CONF.stop();
                }));
  }

  @SneakyThrows
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    DB_CONF.configureProperties(registry);
    RABBITMQ_CONF.configureProperties(registry);
    EMAIL_CONF.configureProperties(registry);

    Class<?> envConfClazz = EnvConf.class;
    var configureMethod =
        envConfClazz.getDeclaredMethod("configureProperties", DynamicPropertyRegistry.class);
    var envConfInstance = envConfClazz.getConstructor().newInstance();
    configureMethod.invoke(envConfInstance, registry);
  }
}
