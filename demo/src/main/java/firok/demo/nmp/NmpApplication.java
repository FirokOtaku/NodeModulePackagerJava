package firok.demo.nmp;

import firok.tool.nmp.NodeModuleSource;
import firok.tool.nmp.NodeModuleSources;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

@NodeModuleSources({
        @NodeModuleSource(value = "beercss", packaging = false),
        @NodeModuleSource("vue"),
})
public class NmpApplication extends Application
{
    @Override
    public void start(Stage stage) throws Exception
    {
        var res = NmpApplication.class.getResource("/firok/demo/nmp/view.fxml");

        var loader = new FXMLLoader(res);
        var root = (Parent) loader.load();
        var controller = loader.<NmpController>getController();
        controller.initWebView();

        var scene = new Scene(root, 320, 320);
        stage.setTitle("Node Module Packager - JavaFX Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
