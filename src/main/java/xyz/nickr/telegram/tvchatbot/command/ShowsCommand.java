package xyz.nickr.telegram.tvchatbot.command;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import xyz.nickr.telegram.tvchatbot.TvChatBot;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;
import xyz.nickr.telepad.util.PaginatedData;

/**
 * @author Nick Robson
 */
public class ShowsCommand extends Command {

    public ShowsCommand() {
        super("shows");
        this.setHelp("gets a list of all tracked shows");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        Map<String, List<String>> links = TvChatBot.getSeriesController().getSeriesLinks();

        List<String> lines = new LinkedList<>();
        links.forEach((k, v) ->
                lines.add("*" + escape(k) + "*: " + escape(String.join(", ", v))));
        lines.sort(String.CASE_INSENSITIVE_ORDER);

        PaginatedData paginatedData = new PaginatedData(lines, 15);
        paginatedData.setParseMode(ParseMode.MARKDOWN);
        paginatedData.send(0, message);
    }

}
