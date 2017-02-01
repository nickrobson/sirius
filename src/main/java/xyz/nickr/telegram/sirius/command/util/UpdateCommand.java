package xyz.nickr.telegram.sirius.command.util;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
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

    private final String GIT_URL = "https://github.com/nickrobson/sirius.git";
    private final File LAST_PULL_FILE = new File(".version");

    public UpdateCommand() {
        super("update");
        this.setHelp("updates the bot");
        this.setPermission("admin.update");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        File buildDir = new File("build");
        try {
            ProcessBuilder gitProcBuilder = new ProcessBuilder();
            if (buildDir.exists()) {
                gitProcBuilder.command("git", "pull").directory(buildDir);
            } else {
                gitProcBuilder.command("git", "clone", GIT_URL, buildDir.toString());
            }
            Process gitProc = gitProcBuilder.start();
            int gitExit = gitProc.waitFor();
            if (gitExit != 0) {
                message.getChat().sendMessage(SendableTextMessage.plain("Git process exited with a non-zero exit code.").replyTo(message).build());
                return;
            }
            Process gitHashProc = new ProcessBuilder()
                    .command("git", "log", "-n", "1", "--pretty=format:%H:%s")
                    .directory(buildDir)
                    .start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(gitHashProc.getInputStream()));
            String[] parts = reader.readLine().split(":", 2);
            reader.close();
            try {
                List<String> lines = Files.readAllLines(LAST_PULL_FILE.toPath());
                if (parts[0].equals(lines.get(0))) {
                    message.getChat().sendMessage(SendableTextMessage.plain("No new updates.").replyTo(message).build());
                    return;
                }
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
                    .directory(buildDir)
                    .redirectErrorStream(true)
                    .start();
            int mvnExit = mvnProc.waitFor();
            List<String> updateOutput = new LinkedList<>();
            BufferedReader gitReader = new BufferedReader(new InputStreamReader(gitProc.getInputStream()));
            BufferedReader mvnReader = new BufferedReader(new InputStreamReader(mvnProc.getInputStream()));
            String line;
            updateOutput.add("\n=== GIT ===\n");
            while ((line = gitReader.readLine()) != null)
                updateOutput.add(line);
            updateOutput.add("\n=== MAVEN ===\n");
            while ((line = mvnReader.readLine()) != null)
                updateOutput.add(line);
            String pasteId = null;
            try {
                pasteId = Unirest.post("https://nickr.xyz/cgi/paste.py")
                        .field("lang", "text")
                        .field("text", String.join("\n", updateOutput))
                        .asString()
                        .getBody();
            } catch (UnirestException e) {
                e.printStackTrace();
            }
            String m = escape(mvnExit == 0 ? "Successfully built new version." : "Maven process exited with a non-zero exit code.");
            if (pasteId != null) {
                String pasteUrl = "https://nickr.xyz/paste/" + pasteId;
                m += "\n" + escape("Git and maven output: ") + "[" + pasteUrl + "](" + pasteUrl + ")";
            }
            message.getChat().sendMessage(SendableTextMessage.markdown(m).replyTo(message).build());
            if (mvnExit != 0)
                return;

            File targetDir = new File(buildDir, "target");
            if (targetDir.isDirectory()) {
                File[] matching = targetDir.listFiles(fn -> fn.getName().endsWith("-with-dependencies.jar"));
                if (matching == null || matching.length == 0) {
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

}
