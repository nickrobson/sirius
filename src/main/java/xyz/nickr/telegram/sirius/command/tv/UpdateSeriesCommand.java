package xyz.nickr.telegram.sirius.command.tv;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
                int size = seriesSet.size();
                AtomicInteger n = new AtomicInteger(0);
                new Thread(() -> {
                    try {
                        while (updating) {
                            edit(m, "_Updating..._ " + escape(n.get() + "/" + size), ParseMode.MARKDOWN);
                            Thread.sleep(3000);
                        }
                        edit(m, "Finished!", ParseMode.NONE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
                for (Series series : seriesSet) {
                    series.update();
                    n.incrementAndGet();
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
