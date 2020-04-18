package sample;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import map.Map;
import map.Place;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import social.User;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
        primaryStage.setTitle("gui");
        primaryStage.setScene(new Scene(root, 800, 600));
        //noinspection Convert2Lambda
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>() {
            @Override
            public void handle(WindowEvent window) {
                try {
                    serve(root);
                } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        });
        primaryStage.show();
    }

    public void startLogModal(Stream<String> logs, Window win) throws Exception{
        AnchorPane root = FXMLLoader.load(getClass().getResource("/logModal.fxml"));
        VBox vBox = (VBox) root.getChildren().get(0);
        Stage primaryStage = new Stage();
        primaryStage.setTitle("user log");
        primaryStage.setScene(new Scene(root, 600, 400));
        //noinspection Convert2Lambda
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>() {
            @Override
            public void handle(WindowEvent window) {
                vBox.getChildren().addAll(logs.map(x -> {
                    var label = new Label(x);
                    label.setMinWidth(600);
                    label.setPrefWidth(600);
                    label.setMaxWidth(600);
                    return label;
                }).toArray(Label[]::new));
            }
        });
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.initOwner(win);
        primaryStage.show();
    }

    public void startUsersModal(Stream<Triplet<User, Color, List<Pair<Place, String>>>> aliveUsers, Window win) throws Exception{
        AnchorPane root = FXMLLoader.load(getClass().getResource("/usersModal.fxml"));
        VBox vBox = (VBox) root.getChildren().get(0);
        Stage primaryStage = new Stage();
        primaryStage.setTitle("user list");
        primaryStage.setScene(new Scene(root, 600, 400));
        //noinspection Convert2Lambda
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>() {
            @Override
            public void handle(WindowEvent window) {
                vBox.getChildren().addAll(aliveUsers.map(x -> {
                    var label = new Label(x.getValue0().getName());
                    label.setTextFill(x.getValue1());
                    label.onMouseClickedProperty().setValue(ex -> {
                        try {
                            startLogModal(
                                    x.getValue2().
                                            stream().
                                            map(y -> String.format("%s at %s", y.getValue0().getName(), y.getValue1())),
                                    ((Node) ex.getSource()).getScene().getWindow()
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    return label;
                }).toArray(Label[]::new));
            }
        });
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.initOwner(win);
        primaryStage.show();
    }

    public void startCheckInModal(Consumer<Place> callback, List<Place> places, Window win) throws Exception{
        AnchorPane root = FXMLLoader.load(getClass().getResource("/checkInModal.fxml"));
        VBox vBox = (VBox) root.getChildren().get(0);
        Stage primaryStage = new Stage();
        primaryStage.setTitle("check in");
        primaryStage.setScene(new Scene(root, 600, 400));
        //noinspection Convert2Lambda
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>() {
            @Override
            public void handle(WindowEvent window) {
                vBox.getChildren().addAll(places.stream().map(x -> {
                    var label = new Label(x.getName());
                    label.onMouseClickedProperty().setValue(ex -> {
                        callback.accept(x);
                        primaryStage.close();
                    });
                    return label;
                }).toArray(Label[]::new));
            }
        });
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.initOwner(win);
        primaryStage.show();
    }

    private void serve(Parent root) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        var ui = Class.forName("ConsoleUI");
        var ui_inst = ui.getDeclaredConstructor().newInstance();
        var map = (Map)ui.getDeclaredField("map").get(ui_inst);
        //noinspection unchecked
        var users = (List<User>)ui.getDeclaredField("users").get(ui_inst);
        var user1 = (User)Utils.getPrivateField(ui_inst, "user1");

        var canvas = (Canvas)((VBox)((ScrollPane) root).getContent()).getChildren().get(1);
        var context = canvas.getGraphicsContext2D();
        var render = new Render(context, map);
        render.startRendering();

        var buttons = ((HBox)((VBox)((ScrollPane) root).getContent()).getChildren().get(0)).getChildren();
        var button_addUser = (Button)buttons.get(0);
        var live = ui.getDeclaredMethod("live");
        button_addUser.onActionProperty().setValue(ex -> {
            var t = new Thread(() -> {
                try {
                    live.setAccessible(true);
                    live.invoke(ui_inst);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
            t.setDaemon(true);
            t.start();
        });
        var button_getUsers = (Button)buttons.get(1);
        button_getUsers.onActionProperty().setValue(ex -> {
            try {
                var aliveUsers = users.
                        stream().
                        filter(User::isAlive).
                        map(x -> Triplet.with(x, render.getColourResolver().resolve(x), x.getLog()));

                startUsersModal(aliveUsers, ((Node)ex.getSource()).getScene().getWindow());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        var button_checkIn = (Button)buttons.get(2);
        button_checkIn.onActionProperty().setValue(ex -> {
            try {
                var places = map.getPlaces();
                startCheckInModal(place -> Utils.checkIn(user1, map, place), places, ((Node)ex.getSource()).getScene().getWindow());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
