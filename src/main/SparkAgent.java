import java.lang.instrument.Instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkAgent {
    static Logger log = LoggerFactory.getLogger(SparkAgent.class);

    private static String className = "org.apache.spark.scheduler.DAGScheduler";

    public static void premain(String agentargs, Instrumentation inst) {
        transformClass(className, inst);
    }

    public static void agentmain(String agentargs, Instrumentation inst) {
        transformClass(className, inst);
    }

    private static void transformClass(String className,
                                       Instrumentation inst) {
        // find the class
        Class<?> targetClass = null;
        ClassLoader targetClassLoader = null;

        log.info("targetClass: " + className);
        try {
            targetClass = Class.forName(className);
            targetClassLoader = targetClass.getClassLoader();
        } catch (Exception ex) {
            log.error("Class [" + className + "] not found with Class.forName");
        }

        if(targetClass == null) {
            for(Class<?> clazz: inst.getAllLoadedClasses()) {
                if(clazz.getName().equals(className)) {
                    targetClass = clazz;
                    targetClassLoader = targetClass.getClassLoader();
                }
            }
        }

        if(targetClass == null)
            throw new RuntimeException("Failed to find class ["
                                       + className + "]");

        transform(targetClass, targetClassLoader, inst);
    }

    private static void transform(Class<?> clazz,
                                  ClassLoader classLoader,
                                  Instrumentation inst) {
        SchedulerTransformer tf = new SchedulerTransformer(clazz.getName(), classLoader);

        inst.addTransformer(tf, true);

        try {
            inst.retransformClasses(clazz);
        } catch (Exception e) {
            throw new RuntimeException("Transform failed for ["
                                       + clazz.getName() + "]", e);
        }
    }
}
