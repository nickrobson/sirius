package xyz.nickr.telegram.sirius.command.omdb;

import java.util.LinkedList;
import java.util.List;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.jomdb.model.SearchResult;
import xyz.nickr.jomdb.model.SearchResults;
import xyz.nickr.jomdb.model.SearchResultsPage;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;
import xyz.nickr.telepad.util.PaginatedData;

/**
 * @author Nick Robson
 */
public class OmdbSearchCommand extends Command {

    public OmdbSearchCommand() {
        super("omdbsearch");
        this.setUsage("[search terms...]");
        this.setHelp("searches omdbapi.com for a series or movie");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length == 0) {
            sendUsage(message);
            return;
        }
        String search = String.join(" ", args);
        SearchResults results = Sirius.getOmdb().search(search);
        if (results == null) {
            message.getChat().sendMessage(
                    SendableTextMessage.plain("Failed to search! Please notify @nickrobson")
                            .replyTo(message)
                            .build()
            );
            return;
        }
        if (results.getPageCount() == 0) {
            message.getChat().sendMessage(
                    SendableTextMessage.plain("No results found.")
                            .replyTo(message)
                            .build()
            );
            return;
        }
        PaginatedData paginatedData = new PaginatedData(i -> {
            SearchResultsPage page = results.getPage(i + 1);
            List<String> lines = new LinkedList<>();
            for (SearchResult result : page) {
                lines.add(String.format("*%s* (%s): %s, a %s [(poster)](%s)", escape(result.getTitle()), escape(result.getYear()), escape(result.getImdbId()), escape(result.getType()), result.getPoster()));
            }
            return String.join("\n", lines);
        }, results.getPageCount());
        paginatedData.setParseMode(ParseMode.MARKDOWN);
        paginatedData.send(0, message);
    }

}
