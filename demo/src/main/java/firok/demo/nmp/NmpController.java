package firok.demo.nmp;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import org.intellij.lang.annotations.Language;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;

public class NmpController
{
    @FXML
    public WebView wv;

    void initWebView()
    {
        try
        {
            System.out.println("测试 WebView");
            var we = wv.getEngine();

            var url = NmpApplication.class.getResource("/firok/demo/nmp/view.html");
            var res = String.valueOf(url);
            System.out.println(res);

            we.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                @Override
                public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State t1)
                {
                    System.out.println(state);
                    if(state == Worker.State.RUNNING)
                    {
                        var urlJar = NmpApplication.class.getResource("/firok/demo/nmp/vue.jar");
                        try(var cl = new URLClassLoader(new URL[] { urlJar }))
                        {
                            var resVueJs = cl.getResource("dist\\vue.js");
                            String contentVue;
                            try(var ifs = resVueJs.openStream())
                            {
                                contentVue = new String(ifs.readAllBytes(), StandardCharsets.UTF_8);
                                System.out.println(contentVue);
                            }
                            we.executeScript(contentVue);

                            @Language("JS")
                            var scriptContent = """
                                const app = new Vue({
                                    el: '#app',
                                    data: {
                                        msg1: 'Hello Vue!',
                                        msg2: 'Click me',
                                    },
                                })
                                """;
                            var objVue = we.executeScript(scriptContent);
                            System.out.println("objVue: " + objVue);
                        }
                        catch(Exception any)
                        {
                            any.printStackTrace(System.err);
                        }
                    }
                }
            });

            we.load(res);

            System.out.println("测试 WebView 完成");
        }
        catch (Throwable any)
        {
            any.printStackTrace(System.err);
            System.out.println("测试 WebView 发生错误");
        }
    }
}
