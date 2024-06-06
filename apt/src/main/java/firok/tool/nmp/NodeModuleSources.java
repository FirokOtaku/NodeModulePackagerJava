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
}
