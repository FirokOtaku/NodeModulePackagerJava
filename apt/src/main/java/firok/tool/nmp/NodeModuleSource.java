package firok.tool.nmp;

import java.lang.annotation.*;

@Target({ })
@Retention(RetentionPolicy.SOURCE)
public @interface NodeModuleSource
{
    /**
     * 需要引入的 node_module 坐标.
     * 如果设定值为 "*" 则会处理 package.json 中定义的所有 node_module.
     * */
    String value();

    /**
     * 是否需要打包为 jar
     * */
    boolean packaging() default true;

    /**
     * 如果需要打包为 jar, 目标文件名是什么;
     * 如果不需要打包为 jar, 目标路径是什么.
     * */
    String target() default Constants.DefaultTarget;

    /**
     * 需要打包哪些文件, 这会覆盖 package.json 中的 files 字段
     * */
    String[] files() default {};
}
