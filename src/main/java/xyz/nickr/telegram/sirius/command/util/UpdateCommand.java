package xyz.nickr.telegram.sirius.command.util;

import com.mashape.unirest.http.Unirest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class UpdateCommand extends Command {

    private final File BUILD_DIR = new File("build");
    private final File LAST_PULL_FILE = new File(BUILD_DIR, ".version");

    public UpdateCommand() {
        super("update");
        this.setHelp("updates the bot");
        this.setPermission("admin.update");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        try {
            ProcessBuilder gitProcBuilder = new ProcessBuilder();
            if (BUILD_DIR.exists()) {
                new ProcessBuilder()
                        .command("git", "checkout", "--")
                        .directory(BUILD_DIR)
                        .start()
                        .waitFor();
                gitProcBuilder.command("git", "pull").directory(BUILD_DIR);
            } else {
                String GIT_URL = "https://github.com/nickrobson/sirius.git";
                gitProcBuilder.command("git", "clone", GIT_URL, BUILD_DIR.toString());
            }
            Process gitProc = gitProcBuilder.start();
            int gitExit = gitProc.waitFor();
            if (gitExit != 0) {
                String pasteId = paste(gitProc);
                String m = escape("Git process exited with a non-zero exit code.");
                if (pasteId != null) {
                    String pasteUrl = "https://nickr.xyz/paste/" + pasteId;
                    m += "\n" + escape("Git output: ") + "[" + pasteUrl + "](" + pasteUrl + ")";
                }
                message.getChat().sendMessage(SendableTextMessage.markdown(m).replyTo(message).build());
                return;
            }
            Process gitHashProc = new ProcessBuilder()
                    .command("git", "log", "-n", "1", "--pretty=format:%H:%s")
                    .directory(BUILD_DIR)
                    .start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(gitHashProc.getInputStream(), StandardCharsets.UTF_8));
            String[] parts = reader.readLine().split(":", 2);
            reader.close();
            try {
                List<String> lines = Files.readAllLines(LAST_PULL_FILE.toPath());
                if (bot.getCollator().equals(parts[0], lines.get(0))) {
                    message.getChat().sendMessage(SendableTextMessage.plain("No new updates.").replyTo(message).build());
                    return;
                }
            } catch (FileNotFoundException | NoSuchFileException ignored) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            message.getChat().sendMessage(
                    SendableTextMessage.markdown(escape("Successfully pulled from repo.") + "\n" +
                            "*" + escape("Latest commit:") + "*" +
                            escape(" " + parts[1] + " (") +
                            "_" + escape(parts[0].substring(0, 6)) + "_" +
                            escape(")"))
                            .replyTo(message)
                            .build()
            );
            Files.write(LAST_PULL_FILE.toPath(), Collections.singletonList(parts[0]), StandardOpenOption.CREATE);
            Process mvnProc = new ProcessBuilder()
                    .command("mvn", "clean", "package")
                    .directory(BUILD_DIR)
                    .redirectErrorStream(true)
                    .start();
            int mvnExit = mvnProc.waitFor();
            String pasteId = paste(gitProc, mvnProc);
            String m = escape((mvnExit == 0) ? "Successfully built new version." : "Maven process exited with a non-zero exit code.");
            if (pasteId != null) {
                String pasteUrl = "https://nickr.xyz/paste/" + pasteId;
                m += "\n" + escape("Git and maven output: ") + "[" + pasteUrl + "](" + pasteUrl + ")";
            }
            message.getChat().sendMessage(SendableTextMessage.markdown(m).replyTo(message).build());
            if (mvnExit != 0)
                return;

            File targetDir = new File(BUILD_DIR, "target");
            if (targetDir.isDirectory()) {
                File[] matching = targetDir.listFiles(fn -> fn.getName().endsWith("-with-dependencies.jar"));
                if ((matching == null) || (matching.length == 0)) {
                    message.getChat().sendMessage(SendableTextMessage.plain("No jar files ending with '-with-dependencies.jar'").replyTo(message).build());
                    return;
                }
                File jarFile = matching[0];
                Files.copy(jarFile.toPath(), new File("sirius.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
                message.getChat().sendMessage(SendableTextMessage.plain("Restarting!").replyTo(message).build());
                System.exit(0);
            } else {
                message.getChat().sendMessage(SendableTextMessage.plain("build/target isn't a directory!").replyTo(message).build());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static String paste(Process... processes) {
        List<String> updateOutput = new LinkedList<>();
        for (Process process : processes) {
            try {
                updateOutput.add("\n=========\n");
                BufferedReader procReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = procReader.readLine()) != null)
                    updateOutput.add(line);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (updateOutput.size() > 1)
            updateOutput.add("\n=========\n");
        String pasteId = null;
        try {
            pasteId = Unirest.post("https://nickr.xyz/cgi/paste.py")
                    .field("lang", "text")
                    .field("text", String.join("\n", updateOutput))
                    .asString()
                    .getBody();
        } catch (Exception ignored) {}
        return pasteId;
    }

}
