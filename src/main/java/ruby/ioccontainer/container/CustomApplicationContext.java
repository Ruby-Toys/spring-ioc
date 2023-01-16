package ruby.ioccontainer.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
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
import java.util.Objects;

public class CustomApplicationContext {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String SUFFIX = "/**/*.class";
    private final Map<Class<?>, Object> container;
    private final MetadataReaderFactory metadataReaderFactory;
    private final ResourcePatternResolver resourcePatternResolver;
    private static CustomApplicationContext applicationContext;

    private CustomApplicationContext(
            Map<Class<?>, Object> container,
            MetadataReaderFactory metadataReaderFactory,
            ResourcePatternResolver resourcePatternResolver) {
        this.container = container;
        this.metadataReaderFactory = metadataReaderFactory;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public static CustomApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            Map<Class<?>, Object> container = new HashMap<>();
            MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            applicationContext = new CustomApplicationContext(container, metadataReaderFactory, resourcePatternResolver);
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
        try {
            Resource[] resources = resourcePatternResolver.getResources(SUFFIX);
            return Arrays.stream(resources)
                    .filter(Resource::isFile)
                    .map(resource -> {
                        Class<?> classType = convertResourceToClassType(resource);

                        if (!classType.isAnnotationPresent(CustomComponentScan.class)) {
                            return null;
                        }

                        // @CustomComponentScan 의 basePackage 값을 설정하지 않았을 경우 애너테이션이 붙어있는 클래스가 위치한 패키지로 설정
                        String basePackage = classType.getAnnotation(CustomComponentScan.class).basePackage();
                        if (basePackage.isBlank()) {
                            basePackage = classType.getPackageName();
                        }

                        logger.info("basePackage : {}", basePackage);
                        return basePackage;
                    })
                    .filter(Objects::nonNull)
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
        String classPattern = basePackage.replaceAll("\\.", "/") + SUFFIX;

        try {
            Resource[] resources = resourcePatternResolver.getResources(classPattern);
            Arrays.stream(resources)
                    .forEach(resource -> {
                        Class<?> classType = convertResourceToClassType(resource);

                        // @CustomComponent 가 붙어있는 클래스를 빈 객체로 생성하여 등록
                        if (classType.isAnnotationPresent(CustomComponent.class)) {
                            createBean(classType);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("빈 등록 중 예외 발생!", e);
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
                                throw new RuntimeException("해당 타입의 빈을 찾을 수 없습니다. : " + field.getType());
                            }

                            field.setAccessible(true);
                            field.set(bean, fieldInstance);

                            logger.info("[{}] injected field: {}", fieldInstance.getClass().getName(), bean.getClass().getName());
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("빈 주입 중 예외 발생!", e);
                        }
                    });
        }
    }

    /** 객체 생성 - 파라미터가 없는 기본 생성자가 반드시 필요함 */
    private <T> void createBean(Class<T> classType) {
        try {
            Constructor<T> constructor = classType.getConstructor();

            constructor.setAccessible(true);

            T bean = constructor.newInstance();
            container.put(classType, bean);

            logger.info("Bean created : {}" , bean.getClass().getName());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> Object getBean(Class<T> classType) {
        return container.get(classType);
    }
}
