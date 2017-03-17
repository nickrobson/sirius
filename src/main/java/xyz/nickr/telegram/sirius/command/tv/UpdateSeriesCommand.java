package xyz.nickr.telegram.sirius.command.tv;

import java.util.Set;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class UpdateSeriesCommand extends Command {

    private boolean updating;

    public UpdateSeriesCommand() {
        super("updateseries");
        this.setHelp("updates the database models for all tracked series");
        this.setPermission("series.update");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (updating) {
            reply(message, "Already updating!", ParseMode.NONE);
            return;
        }
        updating = true;
        new Thread(() -> {
            Message m = reply(message, "_Updating..._", ParseMode.MARKDOWN);
            try {
                Set<Series> seriesSet = Sirius.getSeriesController().getSeries();
                int n = 0, size = seriesSet.size();
                edit(m, "_Updating..._ " + escape("0/" + size), ParseMode.MARKDOWN);
                for (Series series : seriesSet) {
                    series.update();
                    if ((n % 5) == 0) {
                        edit(m, "_Updating..._ " + escape(++n + "/" + size), ParseMode.MARKDOWN);
                    }
                }
                reply(message, "Successfully updated all tracked shows!", ParseMode.NONE);
            } catch (Exception ex) {
                edit(m, "Failed to update tracked shows - is filmfo down?", ParseMode.NONE);
                ex.printStackTrace();
            } finally {
                updating = false;
            }
        }).start();
    }

}
