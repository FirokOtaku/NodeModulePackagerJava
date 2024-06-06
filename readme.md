# ![logo](logo.svg "Node Module Packager Java")

## 介绍

这是一个 Java APT, 可以在编译期帮你将目录下的 Node.js 依赖一起打包.

此工具 **不会** 自动下载 Node.js 和 Node.js 依赖, 需要你手动安装 Node.js 并使用 `npm install xxx` 指令安装依赖.

简单的应用场景:

* 使用 GraalJS 直接运行 JavaScript 脚本
* 在 JavaFX 的 WebView 中运行 Vue.js 等库

项目基于 Java 21, 以木兰宽松许可证第二版开源.

## 使用

1. 安装 Node Module Packager.  
   可以直接 git clone 项目并在 apt 目录运行 `mvn install`;    
   或使用 Node Module Packager 的 GitHub Maven repo, 类似下面这样
   ```xml
   <repositories>
     <repository>
       <id>github</id>
       <url>https://maven.pkg.github.com/FirokOtaku/NodeModulePackagerJava</url>
     </repository>
   </repositories>
   ```
2. 在需要打包 Node.js 依赖的 Java 项目添加 Node Module Packager 依赖
   ```xml
   <dependency>
     <groupId>firok.tool</groupId>
     <artifactId>node-module-packager-java</artifactId>
     <version>${VERSION}</version>
     <scope>compile</scope>
   </dependency>
   ```
3. 在需要打包 Node.js 依赖的 Java 项目安装需要打包 Node.js 依赖
   ```bash
   npm install xxx
   ```
4. 在项目任意类 (一般在主类) 声明并配置注解
   ```java
   import firok.tool.nmp.NodeModuleSource;
   import firok.tool.nmp.NodeModuleSources;

   @NodeModuleSources({
     @NodeModuleSource("xxx")
   })
   public class Main
   {
     public static void main(String[] args) { }
   }
   ```
5. 编译项目, 配置的依赖项会作为资源文件一并编译

> [demo](demo) 目录即为示例项目
