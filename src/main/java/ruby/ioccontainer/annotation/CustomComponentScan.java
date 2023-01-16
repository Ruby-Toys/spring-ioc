package ruby.ioccontainer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CustomComponentScan
 * - basePackage 및 하위 패키지까지 컴포넌트 스캔
 * - basePackage 값을 설정하지 않을 경우 해당 애너테이션이 붙은 클래스가 있는 패키지를 값으로 설정
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CustomComponentScan {
    String basePackage() default "";
}
