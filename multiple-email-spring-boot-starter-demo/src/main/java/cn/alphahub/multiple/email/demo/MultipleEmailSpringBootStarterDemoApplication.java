package cn.alphahub.multiple.email.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class MultipleEmailSpringBootStarterDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultipleEmailSpringBootStarterDemoApplication.class, args);
    }

}
