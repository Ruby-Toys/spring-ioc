package ruby.container;

import ruby.ioccontainer.annotation.CustomAutowired;
import ruby.ioccontainer.annotation.CustomComponent;

@CustomComponent
public class TestBean1 {

    @CustomAutowired
    private TestBean2 testBean2;

    private TestBean3 testBean3;

    public TestBean2 getTestBean2() {
        return testBean2;
    }

    public TestBean3 getTestBean3() {
        return testBean3;
    }
}
