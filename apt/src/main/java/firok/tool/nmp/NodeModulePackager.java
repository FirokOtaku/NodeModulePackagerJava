package firok.tool.nmp;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@SupportedAnnotationTypes({"firok.tool.nmp.NodeModuleSources"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions(Constants.OptionBasedir)
public class NodeModulePackager extends AbstractProcessor
{
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    public void printNote(Object obj)
    {
        messager.printMessage(Diagnostic.Kind.NOTE,"[NMP] " + obj);
    }
    public void printWarning(Object obj)
    {
        messager.printMessage(Diagnostic.Kind.WARNING,"[NMP] " + obj);
    }
    public void printError(Object obj)
    {
        messager.printMessage(Diagnostic.Kind.ERROR,"[NMP] " + obj);
    }

    private File pathRoot; // 正在编译的项目根目录
    private File pathNmpCache; // nmp 的缓存目录
    private File pathRealNodeModules; // 所有的 node_modules
    private File getNodeModuleFolder(String name) // 获取某个 node_module 的真实目录
    {
        return new File(pathRealNodeModules, name);
    }
    private File getNodeModuleCache(String name, String version) // 获取某个 node_module 的缓存文件
    {
        return new File(pathNmpCache, name + "-" + version + ".bin");
    }

    @Override
    public synchronized void init(ProcessingEnvironment pe) {
        super.init(pe);
        typeUtils = pe.getTypeUtils();
        elementUtils = pe.getElementUtils();
        filer = pe.getFiler();
        messager = pe.getMessager();

        messager.printMessage(Diagnostic.Kind.NOTE,"NMP ADT 初始化完成");

        var rawBasedir = processingEnv.getOptions().get(Constants.OptionBasedir);
        if(rawBasedir == null || rawBasedir.isEmpty())
        {
            printError("未配置 npm.basedir 环境变量");
        }

        try
        {
            assert rawBasedir != null;
            var temp = new File(rawBasedir).getCanonicalFile();

            if(!temp.isDirectory())
                throw new RuntimeException();

            pathRoot = temp;
            pathNmpCache = new File(pathRoot, ".nmp").getCanonicalFile();
            pathRealNodeModules = new File(pathRoot, "node_modules").getCanonicalFile();
        }
        catch (Exception any)
        {
            printError("npm.basedir 解析错误, 无法解析为一个已存在的目录: " + rawBasedir);
        }

        //noinspection ResultOfMethodCallIgnored
        pathNmpCache.mkdirs();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment re)
    {
        var pathNodeModules = new File(pathRoot, "node_modules");
        var pathCache = new File(pathRoot, ".nmp"); // 缓存目录
        var fileCacheIndex = new File(pathCache, "index.json");

        var eles = re.getElementsAnnotatedWith(NodeModuleSources.class);
        // 每个 NodeModuleSource 视为一个处理单元
        eles.forEach(ele -> {
            // 简单读取一下类信息
            var eleClass = ele.asType();
            var eleFullClassName = eleClass.toString(); // 类全限定名

            try
            {
                var om = new ObjectMapper();

                // 从类全限定名里提取 package 名
                var lastDotIndex = eleFullClassName.lastIndexOf('.');
                var packageName = lastDotIndex >= 0 ? eleFullClassName.substring(0, lastDotIndex) : "";

                printNote("处理 Node 依赖项: " + eleFullClassName);
                var anno = ele.getAnnotation(NodeModuleSources.class);

                for(var source : anno.value())
                {
                    var nmName = source.value();
                    var nmPackaging = source.packaging();
                    var nmTarget = source.target();

                    // 读取真实 node_module 信息
                    var folderNodeModule = new File(pathRealNodeModules, nmName);
                    var pathNodeModule = folderNodeModule.toPath();

                    if(nmPackaging) // 打包形式
                    {
                        if(Constants.DefaultTarget.equals(nmTarget))
                            nmTarget = nmName + ".jar";

                        var fileNodeModulePackageJson = new File(folderNodeModule, "package.json");
                        var beanPackageJson = om.readValue(fileNodeModulePackageJson, PackageInfo.class);

                        var nmVersion = beanPackageJson.version;

                        // 检查缓存状态
                        var fileCachedNodeModule = getNodeModuleCache(nmName, nmVersion);
                        if(!fileCachedNodeModule.exists()) // 开始创建缓存
                        {
                            fileCachedNodeModule.getParentFile().mkdirs();
                            fileCachedNodeModule.createNewFile();
                            try(var ofs = new FileOutputStream(fileCachedNodeModule);
                                var ozs = new ZipOutputStream(ofs);
                                var paths = Files.walk(pathNodeModule)
                                        .filter(Files::isRegularFile)
                            )
                            {
                                for(var pathFile : paths.toList())
                                {
                                    var relativePath = pathNodeModule.relativize(pathFile);
                                    var entryName = relativePath.toString();
                                    var entry = new ZipEntry(entryName);
                                    ozs.putNextEntry(entry);
                                    try(var ifs = new FileInputStream(pathFile.toFile()))
                                    {
                                        ifs.transferTo(ozs);
                                    }
                                    ozs.closeEntry();
                                }
                                ozs.finish();
                                ozs.flush();
                            }
                        }
                        // 把缓存复制到目标目录
                        var targetFile = filer.createResource(
                                StandardLocation.CLASS_OUTPUT,
                                packageName,
                                nmTarget
                        );
                        try(var ifs = new FileInputStream(fileCachedNodeModule);
                            var ofs = targetFile.openOutputStream())
                        {
                            ifs.transferTo(ofs);
                        }
                    }
                    else // 不打包, 直接把所有文件输出为资源文件
                    {
                        if(Constants.DefaultTarget.equals(nmTarget))
                            nmTarget = packageName + "." + nmName;

                        try(var paths = Files.walk(pathNodeModule))
                        {
                            for(var path : paths.toList())
                            {
                                var isFile = Files.isRegularFile(path);
                                var isDir = Files.isDirectory(path);

                                if(isFile)
                                {
                                    var relativePath = pathNodeModule.relativize(path)
                                            .toString()
                                            .replaceAll("\\\\", "/");

                                    if(relativePath.contains("/"))
                                    {
                                        var index = relativePath.lastIndexOf("/");
                                        var folder = relativePath.substring(0, index);
                                        var file = relativePath.substring(index + 1);

                                        nmTarget = nmTarget + "." + folder.replaceAll("/", ".");
                                        relativePath = file;
                                    }

                                    var targetFile = filer.createResource(
                                            StandardLocation.CLASS_OUTPUT,
                                            nmTarget,
                                            relativePath
                                    );
                                    try(var ifs = new FileInputStream(path.toFile());
                                        var ofs = targetFile.openOutputStream())
                                    {
                                        ifs.transferTo(ofs);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception any)
            {
                printError("处理 " + eleFullClassName + " 的 Node 依赖项时发生未知错误: " + any.getLocalizedMessage());
            }
        });

        return true;
    }
}
