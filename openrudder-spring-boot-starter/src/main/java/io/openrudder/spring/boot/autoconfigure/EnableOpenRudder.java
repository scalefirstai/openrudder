package io.openrudder.spring.boot.autoconfigure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(OpenRudderAutoConfiguration.class)
public @interface EnableOpenRudder {
}
