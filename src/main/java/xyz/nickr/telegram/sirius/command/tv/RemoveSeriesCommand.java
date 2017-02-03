package xyz.nickr.telegram.sirius.command.tv;

import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.jomdb.JavaOMDB;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class RemoveSeriesCommand extends Command {

    public RemoveSeriesCommand() {
        super("delseries");
        this.setHelp("removes a series from the list of tracked series");
        this.setUsage("[name]");
        this.setPermission("series.remove");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length != 1) {
            sendUsage(message);
        } else {
            Series series = Sirius.getSeriesController().getSeriesByLink(args[0]);
            if (series == null) {
                message.getChat().sendMessage(SendableTextMessage.plain("I'm not tracking a series called that.").replyTo(message).build());
                return;
            }
            if (Sirius.getSeriesController().removeShow(series.getId())) {
                message.getChat().sendMessage(SendableTextMessage.plain("Successfully removed " + escape(series.getName()) + "!").replyTo(message).build());
            } else {
                message.getChat().sendMessage(SendableTextMessage.plain("I'm not tracking a series called that.").replyTo(message).build());
            }
        }
    }

}
