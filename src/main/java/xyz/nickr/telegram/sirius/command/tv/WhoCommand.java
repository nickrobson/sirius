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
public class WhoCommand extends Command {

    public WhoCommand() {
        super("who");
        this.setHelp("gets your progress on all shows");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        Map<String, String> progress = Sirius.getProgressController().getProgress(message.getSender());

        List<String> lines = new LinkedList<>();
        for (Map.Entry<String, String> entry : progress.entrySet()) {
            String seriesName = Sirius.getSeriesController().getSeriesName(entry.getKey());
            lines.add(String.format("*%s*: %s", escape(seriesName), escape(entry.getValue())));
        }
        lines.sort(String.CASE_INSENSITIVE_ORDER);

        PaginatedData paginatedData = new PaginatedData(lines, 15);
        paginatedData.setParseMode(ParseMode.MARKDOWN);
        paginatedData.send(0, message);
    }

}
