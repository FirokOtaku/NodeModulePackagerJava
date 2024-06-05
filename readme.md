# Node Module Packager Java

## 介绍

这是一个 Java APT, 工作于编译期, 会读取目录下的 `package.json` 并将相关依赖打包到目标 jar 中.

此工具 **不会** 自动下载 Node.js 和 Node.js 依赖, 需要你手动安装 Node.js 并使用 `npm install xxx` 指令安装依赖.

简单的应用场景:

* 使用 GraalJS 直接运行 JavaScript 脚本
* 在 JavaFX 的 WebView 中调用 Vue.js 等库

## 使用

1. 在 Java 项目添加依赖
   ```xml
   <dependency>
     <groupId>firok.tool</groupId>
     <artifactId>node-module-packager-java</artifactId>
     <version>${VERSION}</version>
     <scope>compile</scope>
   </dependency>
   ```
2. 安装 Node.js 依赖
   ```bash
   npm install xxx
   ```
3. 在项目任意类 (一般在主类) 声明并配置注解
   ```java
   @NodeModuleSources(
     // 配置项...
   )
   public class Main
   {
     public static void main(String[] args) { }
   }
   ```
4. 编译项目, 配置的依赖项会作为资源文件一并编译

## 详细配置方式


