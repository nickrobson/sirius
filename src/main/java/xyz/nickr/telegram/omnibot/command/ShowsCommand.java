package xyz.nickr.telegram.omnibot.command;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.util.LinkedList;
import java.util.List;
import org.bson.Document;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import xyz.nickr.telegram.omnibot.OmniBot;
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
        MongoCollection<Document> showsCollection = OmniBot.getMongoController().getCollection("shows");

        List<String> lines = new LinkedList<>();
        try (MongoCursor<Document> cursor = showsCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();

                String name = document.getString("name");
                List<String> links = (List<String>) document.get("links");

                lines.add("*" + escape(name) + "*: " + escape(String.join(", ", links)));
            }
        }
        lines.sort(String.CASE_INSENSITIVE_ORDER);

        PaginatedData paginatedData = new PaginatedData(lines, 15);
        paginatedData.setParseMode(ParseMode.MARKDOWN);
        paginatedData.send(0, message);
    }

}
