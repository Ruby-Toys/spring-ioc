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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /** ??????????????? ??? ?????? ?????? ??? ????????? ?????? */
    public void init() {
        String basePackage = getBasePackage();

        if (basePackage != null) {
            List<? extends Class<?>> classTypes = scan(basePackage);

            createBeans(classTypes);

            injectBeans();
        }
    }

    /** ???????????? ????????? ??? ????????? ??? ???????????? ?????? */
    private String getBasePackage() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(SUFFIX);
            return Arrays.stream(resources)
                    .filter(Resource::isFile)
                    .map(this::convertResourceToClassType)
                    .filter(classType -> classType.isAnnotationPresent(CustomComponentScan.class))
                    .map(classType -> {
                        // @CustomComponentScan ??? basePackage ?????? ???????????? ????????? ?????? ?????????????????? ???????????? ???????????? ????????? ???????????? ??????
                        String basePackage = classType.getAnnotation(CustomComponentScan.class).basePackage();
                        if (basePackage.isBlank()) {
                            basePackage = classType.getPackageName();
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

    /** Resource ??????????????? ????????? ????????? ?????? */
    private Class<?> convertResourceToClassType(Resource resource) {
        try {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            ClassMetadata classMetadata = metadataReader.getClassMetadata();

            return Class.forName(classMetadata.getClassName());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /** basePackage ??? ???????????? ?????? ??????????????? ?????? ???????????? @Component ??? ?????? ???????????? ?????? */
    private List<? extends Class<?>> scan(String basePackage) {
        String classPattern = basePackage.replaceAll("\\.", "/") + SUFFIX;

        try {
            Resource[] resources = resourcePatternResolver.getResources(classPattern);
            return Arrays.stream(resources)
                    .map(this::convertResourceToClassType)
                    .filter(classType -> classType.isAnnotationPresent(CustomComponent.class))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("??? ?????? ??? ?????? ??????!", e);
        }
    }

    /** ??? ????????? @CustomAutowired ??? ?????? ????????? ??????????????? ????????? ??? ????????? ?????? */
    private void injectBeans() {
        for (Class<?> classType : container.keySet()) {
            Object bean = container.get(classType);

            Arrays.stream(classType.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(CustomAutowired.class))
                    .forEach(field -> {
                        try {
                            Object fieldInstance = container.get(field.getType());
                            if (fieldInstance == null) {
                                throw new RuntimeException("?????? ????????? ?????? ?????? ??? ????????????. : " + field.getType());
                            }

                            field.setAccessible(true);
                            field.set(bean, fieldInstance);

                            logger.info("[{}] injected field: {}", fieldInstance.getClass().getName(), bean.getClass().getName());
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("??? ?????? ??? ?????? ??????!", e);
                        }
                    });
        }
    }

    /** ?????? ?????? - ??????????????? ?????? ?????? ???????????? ????????? ????????? */
    private void createBeans(List<? extends Class<?>> classTypes) {
        classTypes.forEach(classType -> {
            try {
                Constructor<?> constructor = classType.getConstructor();

                constructor.setAccessible(true);

                Object bean = constructor.newInstance();
                container.put(classType, bean);

                logger.info("Bean created : {}" , bean.getClass().getName());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** ??? ?????? ?????? */
    public <T> Object getBean(Class<T> classType) {
        return container.get(classType);
    }
}
