package ruby.ioccontainer;

import ruby.ioccontainer.annotation.CustomComponentScan;
import ruby.ioccontainer.container.CustomApplicationContext;

@CustomComponentScan
public class IocContainerApplication {

	public static void main(String[] args) {
		CustomApplicationContext customApplicationContext = CustomApplicationContext.getApplicationContext();
		customApplicationContext.init();
	}
}
