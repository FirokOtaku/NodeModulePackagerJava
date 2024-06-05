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
     * 是否需要将此 node_module 依赖的 node_module 一并添加.
     * */
    boolean dependencies() default true;
}
