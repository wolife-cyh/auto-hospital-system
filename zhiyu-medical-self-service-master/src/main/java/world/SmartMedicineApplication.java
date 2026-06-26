package world;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类
 *
 */
@SpringBootApplication
@MapperScan("world.dao")
public class SmartMedicineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMedicineApplication.class, args);
    }

}
