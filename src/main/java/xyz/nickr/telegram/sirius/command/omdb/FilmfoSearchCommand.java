package xyz.nickr.telegram.sirius.command.omdb;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.filmfo.model.search.GeneralSearchResults;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;
import xyz.nickr.telepad.util.PaginatedData;

/**
 * @author Nick Robson
 */
public class FilmfoSearchCommand extends Command {

    public FilmfoSearchCommand() {
        super("search");
        this.setUsage("[search terms...]");
        this.setHelp("searches filmfo for a series or movie");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length == 0) {
            sendUsage(message);
            return;
        }
        Sirius.getExecutor().submit(() -> {
            String search = String.join(" ", args);
            GeneralSearchResults results;
            try {
                results = Sirius.getFilmfo().searchEverything(search);
            } catch (Exception ignored) {
                message.getChat().sendMessage(
                        SendableTextMessage.plain("Failed to search! Please notify @nickrobson")
                                .replyTo(message)
                                .build()
                );
                return;
            }
            if (results == null || results.getResults().length == 0) {
                message.getChat().sendMessage(
                        SendableTextMessage.plain("No results found.")
                                .replyTo(message)
                                .build()
                );
                return;
            }
            List<String> lines = Arrays.stream(results.getResults())
                    .map(result -> {
                        if (result.hasYear() && result.hasPoster()) {
                            return String.format(
                                    "*%s* (%s): %s, a %s [(poster)](%s)",
                                    escape(result.getName()),
                                    escape(String.valueOf(result.getYear())),
                                    escape(result.getId()),
                                    escape(result.getType()),
                                    result.getPoster().getUrl());
                        } else if (result.hasYear()) {
                            return String.format(
                                    "*%s* (%s): %s, a %s",
                                    escape(result.getName()),
                                    escape(String.valueOf(result.getYear())),
                                    escape(result.getId()),
                                    escape(result.getType()));
                        } else if (result.hasPoster()) {
                            return String.format(
                                    "*%s*: %s, a %s [(poster)](%s)",
                                    escape(result.getName()),
                                    escape(result.getId()),
                                    escape(result.getType()),
                                    result.getPoster().getUrl());
                        } else {
                            return String.format(
                                    "*%s*: %s, a %s",
                                    escape(result.getName()),
                                    escape(result.getId()),
                                    escape(result.getType()));
                        }
                    }).collect(Collectors.toList());
            PaginatedData paginatedData = new PaginatedData(lines, 15);
            paginatedData.setParseMode(ParseMode.MARKDOWN);
            paginatedData.send(0, message);
        });
    }

}
