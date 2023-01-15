package ruby.ioccontainer.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import ruby.ioccontainer.annotation.CustomAutowired;
import ruby.ioccontainer.annotation.CustomComponent;
import ruby.ioccontainer.annotation.CustomComponentScan;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CustomApplicationContext {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String CLASS_PATH = "classpath*:";
    private static final String SUFFIX = "/**/*.class";
    private static final Map<Class<?>, Object> container = new HashMap<>();
    private final SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    private final PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private static CustomApplicationContext applicationContext;

    private CustomApplicationContext() {}

    public static CustomApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            applicationContext = new CustomApplicationContext();
        }

        return applicationContext;
    }

    /** 컨테이너에 빈 객체 등록 및 의존성 주입 */
    public void init() {
        String basePackage = getBasePackage();

        if (basePackage != null) {
            scan(basePackage);
            injectBean();
        }
    }

    /** @CustomComponentScan 을 통해 컴포넌트 스캔을 할 기준이 될 패키지를 확인 */
    private String getBasePackage() {
        String packageSearchPathPattern = CLASS_PATH + SUFFIX;
        try {
            Resource[] resources = resourcePatternResolver.getResources(packageSearchPathPattern);
            return Arrays.stream(resources)
                    .filter(resource -> {
                        if (!resource.isFile()) {
                            return false;
                        }

                        Class<?> classType = convertResourceToClassType(resource);
                        return classType.isAnnotationPresent(CustomComponentScan.class);
                    })
                    .map(resource -> {
                        Class<?> classType = convertResourceToClassType(resource);

                        String basePackage = classType.getAnnotation(CustomComponentScan.class).basePackage();
                        // CustomComponentScan 의 value 값이 설정되어 있지 않다면 애너테이션이 붙은 클래스의 패키지를 기준으로 한다.
                        if (basePackage.isBlank()) {
                            int splitIndex = classType.getName().lastIndexOf(".");
                            basePackage =  classType.getName().substring(0, splitIndex);
                        }

                        logger.info("basePackage : {}", basePackage);
                        return basePackage;
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Resource 객체로부터 클래스 정보를 획득 */
    private Class<?> convertResourceToClassType(Resource resource) {
        try {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            ClassMetadata classMetadata = metadataReader.getClassMetadata();
            return Class.forName(classMetadata.getClassName());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /** basePackage 를 기준으로 하위 패키지까지 모두 탐색하여 @Component 가 붙은 클래스를 빈 객체로 생성하여 등록 */
    private void scan(String basePackage) {
        String packageSearchPathPattern =
                CLASS_PATH
                + basePackage.replaceAll("\\.", "/")
                + SUFFIX;

        try {
            Resource[] resources = resourcePatternResolver.getResources(packageSearchPathPattern);
            Arrays.stream(resources).forEach(resource -> {
                try {
                    Class<?> classType = convertResourceToClassType(resource);

                    // @CustomComponent 가 붙어있는 클래스를 빈 객체로 생성하여 등록
                    if (classType.isAnnotationPresent(CustomComponent.class)) {
                        createBean(classType);
                    }
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 의존성 주입 - 빈 객체의 @CustomAutowired 가 붙은 필드에 컨테이너에 저장된 빈 객체를 주입 */
    private void injectBean() {
        for (Class<?> classType : container.keySet()) {
            Object bean = container.get(classType);

            Arrays.stream(classType.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(CustomAutowired.class))
                    .forEach(field -> {
                        try {
                            Object fieldInstance = container.get(field.getType());
                            if (fieldInstance == null) {
                                throw new RuntimeException("Not created bean");
                            }

                            field.setAccessible(true);
                            field.set(bean, fieldInstance);

                            logger.info("[{}] injected field: {}", fieldInstance.getClass().getName(), bean.getClass().getName());
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    /** 객체 생성 - 파라미터가 없는 기본 생성자가 반드시 필요함 */
    private <T> void createBean(Class<T> classType) throws NoSuchMethodException {
        try {
            Constructor<T> constructor = classType.getConstructor();

            constructor.setAccessible(true);

            T bean = constructor.newInstance();
            container.put(classType, bean);

            logger.info("Bean create : {}" , bean.getClass().getName());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Object getBean(Class<T> classType) {
        return container.get(classType);
    }
}
