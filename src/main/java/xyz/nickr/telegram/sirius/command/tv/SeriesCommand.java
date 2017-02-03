package xyz.nickr.telegram.sirius.command.tv;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;
import xyz.nickr.telepad.util.PaginatedData;

/**
 * @author Nick Robson
 */
public class SeriesCommand extends Command {

    public SeriesCommand() {
        super("series");
        this.setHelp("gets a list of all tracked series");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        Map<String, List<String>> links = Sirius.getSeriesController().getSeriesLinks();

        List<String> lines = new LinkedList<>();
        links.forEach((k, v) ->
                lines.add(String.format("*%s*: %s", escape(k), escape(String.join(", ", v)))));
        lines.sort(String.CASE_INSENSITIVE_ORDER);

        PaginatedData paginatedData = new PaginatedData(lines, 15);
        paginatedData.setParseMode(ParseMode.MARKDOWN);
        paginatedData.send(0, message);
    }

}
