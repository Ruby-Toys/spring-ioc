package ruby.ioccontainer.bean;

import ruby.ioccontainer.annotation.CustomAutowired;
import ruby.ioccontainer.annotation.CustomComponent;

@CustomComponent
public class Bean1 {

    @CustomAutowired
    private Bean2 bean2;

    private Bean3 bean3;

    public Bean2 getBean2() {
        return bean2;
    }

    public Bean3 getBean3() {
        return bean3;
    }
}
