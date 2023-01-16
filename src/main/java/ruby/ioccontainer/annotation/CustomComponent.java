package ruby.ioccontainer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CustomComponent
 * - 컴포넌트 스캔 내의 해당 애너테이션이 붙은 클래스를 빈 객체로 생성하여 등록
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CustomComponent {
}
