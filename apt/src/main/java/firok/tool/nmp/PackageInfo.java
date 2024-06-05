package firok.tool.nmp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import java.util.HashMap;

/**
 * 用来映射 package.json 的实体类
 * */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PackageInfo
{
    String version;
    String name;
    HashMap<String, String> dependencies;
    String[] files;
}
