package ruby.testioc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ruby.ioccontainer.container.CustomApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class CustomComponentScanTest {

    @Test
    @DisplayName("빈 객체 생성")
    void testGetBean() {
        CustomApplicationContext applicationContext = CustomApplicationContext.getApplicationContext();
        applicationContext.init();

        Object bean1 = applicationContext.getBean(TestBean1.class);
        Object bean2 = applicationContext.getBean(TestBean2.class);
        Object bean3 = applicationContext.getBean(TestBean3.class);

        assertThat(bean1.getClass()).isEqualTo(TestBean1.class);
        assertThat(bean2.getClass()).isEqualTo(TestBean2.class);
        assertThat(bean3).isNull();
    }

    @Test
    @DisplayName("의존성 주입 확인")
    void testInject() {
        CustomApplicationContext applicationContext = CustomApplicationContext.getApplicationContext();
        applicationContext.init();

        TestBean1 bean1 = (TestBean1) applicationContext.getBean(TestBean1.class);
        assertThat(bean1.getTestBean2()).isNotNull();
        assertThat(bean1.getTestBean3()).isNull();
    }
}