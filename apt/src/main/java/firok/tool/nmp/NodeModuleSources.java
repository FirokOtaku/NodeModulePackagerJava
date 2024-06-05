package firok.tool.nmp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface NodeModuleSources
{
    /**
     * 需要处理的模块列表
     * */
    NodeModuleSource[] value() default {};

    /**
     * 是否需要将所有的 node_module 打包为 jar
     * */
    boolean packaging() default true;

    /**
     * 如果需要打包为 jar, 目标文件名是什么
     * */
    String target() default "node_modules.jar";
}
