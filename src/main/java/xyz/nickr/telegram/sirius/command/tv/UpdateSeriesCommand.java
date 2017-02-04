package xyz.nickr.telegram.sirius.command.tv;

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

    public UpdateSeriesCommand() {
        super("updateseries");
        this.setHelp("updates the database models for all tracked series");
        this.setPermission("series.update");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        Message m = message.getChat().sendMessage(SendableTextMessage.markdown("_Updating..._").replyTo(message).build());
        for (Series series : Sirius.getSeriesController().getSeries()) {
            series.update();
        }
        message.getBotInstance().editMessageText(m, "Successfully updated all tracked shows", ParseMode.NONE, true, null);
    }

}
