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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@SupportedAnnotationTypes({"firok.tool.nmp.NodeModuleSources"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions("project.basedir")
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

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();

        messager.printMessage(Diagnostic.Kind.NOTE,"NMP ADT 初始化完成");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment re)
    {
        try
        {
            var basedir = processingEnv.getOptions().get("project.basedir");
            if(basedir == null || basedir.isEmpty())
            {
                printError("无法获取项目路径, 请确保已经在配置了环境变量 project.basedir. 详情请查阅使用文档");
            }
            assert basedir != null;

            var pathRoot = new File(basedir);
            var pathNodeModules = new File(pathRoot, "node_modules");
            var pathCache = new File(pathRoot, ".nmp");
            var fileCacheIndex = new File(pathCache, "index.json");

            var eles = re.getElementsAnnotatedWith(NodeModuleSources.class);
            for(var ele : eles)
            {
                var eleType = ele.asType();
                var eleName = eleType.toString();
                printNote("处理元素: " + eleName);
                var anno = ele.getAnnotation(NodeModuleSources.class);

                var pathCacheIndex = new File(pathRoot, eleName + "-index.json");
                var pathCacheInstance = new File(pathCache, eleName + "-cache.bin");

                processInternal(
                        pathRoot,
                        pathNodeModules,

                        pathCache,
                        pathCacheIndex,
                        pathCacheInstance,

                        eleName,
                        anno
                );
            }
        }
        catch (Exception any)
        {
            printError("发生错误");
            printError(any);
        }

        return true;
    }

    private interface Handler
    {
        void handle(Set<String> set, String name, boolean dep) throws Exception;
    }

    private void copyToZip(String[] trees, File source, ZipOutputStream ozs) throws IOException
    {
        var filename = source.getName();
        var parts = new ArrayList<>(Arrays.asList(trees));
        parts.add(filename);
        if(source.isFile())
        {
            var parts2 = new ArrayList<>(parts);
            parts2.remove(0);
            var entryName = String.join("/", parts2);

            var entry = new ZipEntry(entryName);
            ozs.putNextEntry(entry);
            try(var ifs = new FileInputStream(source))
            {
                ifs.transferTo(ozs);
            }
            ozs.closeEntry();
        }
        if(source.isDirectory())
        {
            var arr = parts.toArray(new String[0]);
            var children = source.listFiles();
            if(children == null) return;
            for(var child : children)
            {
                copyToZip(arr, child, ozs);
            }
        }
    }
    private void copyToRaw(String[] trees, File source) throws IOException
    {
        var filename = source.getName();
        var parts = new ArrayList<>(Arrays.asList(trees));
        parts.add(filename);
        if(source.isFile())
        {
            var targetFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "", String.join("/", parts));
            try(var ifs = new FileInputStream(source);
                var ofs = targetFile.openOutputStream())
            { ifs.transferTo(ofs); }
        }
        if(source.isDirectory())
        {
            var arr = parts.toArray(new String[0]);
            var children = source.listFiles();
            if(children == null) return;
            for(var child : children)
            {
                copyToRaw(arr, child);
            }
        }
    }

    private void processInternal(
            File pathRoot,
            File pathNodeModules,

            File pathCache,
            File fileCacheIndex,
            File fileCacheInstance,

            String eleName,
            NodeModuleSources anno
    ) throws IOException
    {
        var om = new ObjectMapper();

        //noinspection ResultOfMethodCallIgnored
        pathCache.mkdirs();

        // 读取
        var rpTarget = anno.target();
        var listNeededNodeModule = anno.value();
        var setAddedDep = new HashSet<String>();
        var mapDepPackageJson = new HashMap<String, PackageInfo>();

        for(var neededNodeModule : listNeededNodeModule)
        {
            var nameDep = neededNodeModule.value();
            var withDep = neededNodeModule.dependencies();

            var pathDep = new File(pathNodeModules, nameDep);

//            var fileDepPackageJson = new File(pathDep, "package.json");
//            var beanDepPackageJson = om.readValue(fileDepPackageJson, PackageInfo.class);
//            ;

            switch (nameDep)
            {
                case "beercss" -> copyToRaw(new String[] { "node_modules" }, pathDep);
                case "vue" -> {
                    var targetSource = filer.createResource(
                            StandardLocation.CLASS_OUTPUT,
                            "",
                            nameDep + ".jar" // todo 调整目标输出文件路径
                    );
                    try(var ofs = targetSource.openOutputStream();
                        var ozs = new ZipOutputStream(ofs))
                    {
                        copyToZip(new String[0], pathDep, ozs);
                        ozs.finish();
                        ozs.flush();
                    }
                    catch (Exception any)
                    {
                        printWarning(any);
                    }
                }

            }

//            copyToRaw(new String[] { "node_modules" }, pathDep);

//            var targetSource = filer.createResource(
//                    StandardLocation.CLASS_OUTPUT,
//                    "",
//                    nameDep + ".jar" // todo 调整目标输出文件路径
//            );
//            try(var ofs = targetSource.openOutputStream();
//                var ozs = new ZipOutputStream(ofs))
//            {
//                copyToZip(new String[0], pathDep, ozs);
//                ozs.finish();
//                ozs.flush();
//            }
//            catch (Exception any)
//            {
//                printWarning(any);
//            }
        }

    }

}
