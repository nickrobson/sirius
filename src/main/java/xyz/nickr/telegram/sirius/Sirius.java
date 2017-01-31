package xyz.nickr.telegram.sirius;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import xyz.nickr.jomdb.JavaOMDB;
import xyz.nickr.telegram.sirius.command.ProgressCommand;
import xyz.nickr.telegram.sirius.command.ShowsCommand;
import xyz.nickr.telegram.sirius.command.SourceCommand;
import xyz.nickr.telegram.sirius.command.UpdateCommand;
import xyz.nickr.telegram.sirius.command.WhoCommand;
import xyz.nickr.telegram.sirius.storage.MongoController;
import xyz.nickr.telegram.sirius.storage.MongoPermissionPredicate;
import xyz.nickr.telegram.sirius.tv.ProgressController;
import xyz.nickr.telegram.sirius.tv.SeriesController;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.CommandManager;

/**
 * @author Nick Robson
 */
public class Sirius {

    @Getter private static TelepadBot botInstance;
    @Getter private static MongoController mongoController;
    @Getter private static SeriesController seriesController;
    @Getter private static ProgressController progressController;
    @Getter private static ExecutorService executor;

    @Getter public static final JavaOMDB omdb = new JavaOMDB();

    public static void main(String[] args) {
        String authToken = args.length > 0 ? args[0] : System.getenv("AUTH_TOKEN");
        botInstance = new TelepadBot(authToken);

        executor = Executors.newFixedThreadPool(24);

        mongoController = new MongoController();
        seriesController = new SeriesController();
        progressController = new ProgressController();

        registerCommands();
        botInstance.getPermissionManager().addPredicate(new MongoPermissionPredicate());
        botInstance.start(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mongoController.getClient().close();
            executor.shutdown();
        }));
    }

    private static void registerCommands() {
        CommandManager manager = botInstance.getCommandManager();

        manager.register(new WhoCommand());
        manager.register(new ShowsCommand());
        manager.register(new ProgressCommand());

        manager.register(new SourceCommand());
        manager.register(new UpdateCommand());
    }

}
